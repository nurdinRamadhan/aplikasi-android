#!/usr/bin/env python3
"""
Mengunggah paket Falak ke Supabase Storage dan mengisi tabel manifest.

Environment yang dibutuhkan:
- SUPABASE_URL
- SUPABASE_SERVICE_ROLE_KEY

Jangan menaruh service role key di repo. Jalankan dari terminal lokal/admin
ketika paket sudah divalidasi.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any


BUCKET = "falak-ephemeris"


def env_required(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise SystemExit(f"Environment {name} belum diisi.")
    return value.rstrip("/")


def request_json(method: str, url: str, key: str, payload: Any | None = None, prefer: str | None = None) -> Any:
    data = None
    headers = {
        "apikey": key,
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json",
    }
    if prefer:
        headers["Prefer"] = prefer
    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            body = response.read()
            if not body:
                return None
            return json.loads(body.decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} gagal: HTTP {error.code}: {detail}") from error


def upload_file(base_url: str, key: str, local_path: Path, storage_path: str, content_type: str) -> None:
    url_path = urllib.parse.quote(f"{BUCKET}/{storage_path}", safe="/")
    url = f"{base_url}/storage/v1/object/{url_path}"
    headers = {
        "apikey": key,
        "Authorization": f"Bearer {key}",
        "Content-Type": content_type,
        "x-upsert": "true",
    }
    request = urllib.request.Request(url, data=local_path.read_bytes(), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=300) as response:
            response.read()
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Upload {storage_path} gagal: HTTP {error.code}: {detail}") from error


def rest_url(base_url: str, table: str, query: str = "") -> str:
    suffix = f"?{query}" if query else ""
    return f"{base_url}/rest/v1/{table}{suffix}"


def chunked(items: list[dict[str, Any]], size: int) -> list[list[dict[str, Any]]]:
    return [items[index : index + size] for index in range(0, len(items), size)]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("paket_dir", type=Path)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--chunk-size", type=int, default=500)
    args = parser.parse_args()

    metadata_path = args.paket_dir / "metadata-database.json"
    if not metadata_path.exists():
        raise SystemExit(f"Tidak menemukan {metadata_path}")

    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    paket = metadata["paket"]
    berkas = metadata["berkas"]
    indeks = metadata["indeks"]

    print(f"Paket: {paket['kode']} ({paket['status']})")
    print(f"Berkas Storage: {len(berkas)}")
    print(f"Baris indeks: {len(indeks)}")
    if args.dry_run:
        return

    base_url = env_required("SUPABASE_URL")
    service_key = env_required("SUPABASE_SERVICE_ROLE_KEY")

    for item in berkas:
        local_path = args.paket_dir / item["nama_berkas"]
        upload_file(base_url, service_key, local_path, item["path_storage"], item["mime_type"])
        print(f"Upload: {item['path_storage']}")

    if paket["status"] == "aktif":
        query = urllib.parse.urlencode(
            {
                "jenis_sumber": f"eq.{paket['jenis_sumber']}",
                "tahun": f"eq.{paket['tahun']}",
                "status": "eq.aktif",
                "kode": f"neq.{paket['kode']}",
            }
        )
        request_json("PATCH", rest_url(base_url, "falak_paket_data", query), service_key, {"status": "arsip"})

    paket_rows = request_json(
        "POST",
        rest_url(base_url, "falak_paket_data", "on_conflict=kode"),
        service_key,
        paket,
        prefer="resolution=merge-duplicates,return=representation",
    )
    if not paket_rows:
        raise RuntimeError("Upsert falak_paket_data tidak mengembalikan row.")
    paket_id = paket_rows[0]["id"]
    print(f"Paket DB: {paket_id}")

    berkas_payload = [{**item, "paket_id": paket_id} for item in berkas]
    berkas_rows = request_json(
        "POST",
        rest_url(base_url, "falak_berkas_data", "on_conflict=path_storage"),
        service_key,
        berkas_payload,
        prefer="resolution=merge-duplicates,return=representation",
    )
    berkas_by_path = {item["path_storage"]: item["id"] for item in berkas_rows}
    print(f"Berkas DB: {len(berkas_rows)}")

    request_json(
        "DELETE",
        rest_url(base_url, "falak_indeks_data", urllib.parse.urlencode({"paket_id": f"eq.{paket_id}"})),
        service_key,
    )

    indeks_payload: list[dict[str, Any]] = []
    default_berkas_id = berkas_by_path.get(paket["path_manifest_storage"])
    for item in indeks:
        berkas_id = default_berkas_id
        pointer = item.get("path_json_pointer") or ""
        if pointer.startswith("/ephemeris_harian"):
            berkas_id = next((value for path, value in berkas_by_path.items() if path.endswith("ephemeris-harian.json")), default_berkas_id)
        elif pointer.startswith("/hilal_lokasi"):
            berkas_id = next((value for path, value in berkas_by_path.items() if path.endswith("hilal-lokasi.json")), default_berkas_id)
        elif pointer.startswith("/halaman_pdf"):
            berkas_id = next((value for path, value in berkas_by_path.items() if path.endswith("halaman-pdf.json.gz")), default_berkas_id)
        indeks_payload.append({**item, "paket_id": paket_id, "berkas_id": berkas_id})

    inserted = 0
    for group in chunked(indeks_payload, args.chunk_size):
        request_json(
            "POST",
            rest_url(base_url, "falak_indeks_data"),
            service_key,
            group,
            prefer="return=minimal",
        )
        inserted += len(group)
        print(f"Indeks DB: {inserted}/{len(indeks_payload)}")

    print("Publish selesai.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1) from exc
