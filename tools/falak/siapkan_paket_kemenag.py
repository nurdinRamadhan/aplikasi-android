#!/usr/bin/env python3
"""
Menyiapkan paket Ephemeris Kemenag agar siap diunggah ke Supabase Storage.

Input utama adalah JSON hasil parse `parse_kemenag_ephemeris.py`. Script ini
memecah data besar menjadi beberapa berkas yang lebih ramah untuk cache Android,
sekaligus membuat manifest, indeks pencarian, dan metadata database.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


BUCKET = "falak-ephemeris"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_json(path: Path, data: Any, pretty: bool) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2 if pretty else None)
        handle.write("\n")


def write_json_gzip(path: Path, data: Any, pretty: bool) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, ensure_ascii=False, indent=2 if pretty else None) + "\n"
    with gzip.open(path, "wt", encoding="utf-8") as handle:
        handle.write(text)


def file_info(path: Path, jenis_berkas: str, nama_tampil: str, wajib: bool, urutan: int, jumlah_record: int) -> dict[str, Any]:
    if path.name.endswith(".json"):
        mime_type = "application/json"
    elif path.name.endswith(".gz"):
        mime_type = "application/gzip"
    elif path.name.endswith(".pdf"):
        mime_type = "application/pdf"
    else:
        mime_type = "application/octet-stream"
    return {
        "jenis_berkas": jenis_berkas,
        "nama_berkas": path.name,
        "nama_tampil": nama_tampil,
        "path_storage": "",
        "mime_type": mime_type,
        "ukuran_bytes": path.stat().st_size,
        "sha256": sha256_file(path),
        "jumlah_record": jumlah_record,
        "wajib_diunduh": wajib,
        "urutan": urutan,
        "status": "aktif",
        "metadata": {},
    }


def build_indeks(data: dict[str, Any]) -> list[dict[str, Any]]:
    indeks: list[dict[str, Any]] = []

    for nomor, block in enumerate(data.get("ephemeris_daily_blocks", [])):
        tanggal = block["date"]
        page = block.get("page")
        indeks.append(
            {
                "tipe_indeks": "tanggal",
                "judul": f"Ephemeris {tanggal}",
                "ringkasan": "Data Matahari dan Bulan per jam UT.",
                "kata_kunci": [tanggal, "ephemeris", "matahari", "bulan", "data harian"],
                "tanggal_data": tanggal,
                "jam_ut": None,
                "nama_lokasi": None,
                "nomor_halaman_pdf": page,
                "path_json_pointer": f"/ephemeris_harian/{nomor}",
                "metadata": {"jenis_data": "ephemeris_harian"},
            }
        )

        hours = sorted(
            {
                row.get("hour_ut")
                for table_name in ("sun", "moon")
                for row in block.get("hourly_table", {}).get(table_name, [])
                if row.get("hour_ut") is not None
            }
        )
        for hour in hours:
            indeks.append(
                {
                    "tipe_indeks": "jam_ut",
                    "judul": f"Ephemeris {tanggal} jam {hour:02d} UT",
                    "ringkasan": "Baris data Matahari dan Bulan pada jam UT tertentu.",
                    "kata_kunci": [tanggal, f"{hour:02d}:00", "jam ut", "matahari", "bulan"],
                    "tanggal_data": tanggal,
                    "jam_ut": hour,
                    "nama_lokasi": None,
                    "nomor_halaman_pdf": page,
                    "path_json_pointer": f"/ephemeris_harian/{nomor}/hourly_table",
                    "metadata": {"jenis_data": "ephemeris_jam"},
                }
            )

    for nomor_tabel, tabel in enumerate(data.get("hilal_location_tables", [])):
        bulan_hijriah = tabel.get("hijri_month_raw") or "Bulan Hijriah"
        page = tabel.get("page")
        indeks.append(
            {
                "tipe_indeks": "bulan_hijriah",
                "judul": f"Hilal {bulan_hijriah}",
                "ringkasan": tabel.get("event_date_raw"),
                "kata_kunci": ["hilal", bulan_hijriah, tabel.get("ijtima_raw") or ""],
                "tanggal_data": None,
                "jam_ut": None,
                "nama_lokasi": None,
                "nomor_halaman_pdf": page,
                "path_json_pointer": f"/hilal_lokasi/{nomor_tabel}",
                "metadata": {"ijtima": tabel.get("ijtima_raw")},
            }
        )
        for nomor_lokasi, row in enumerate(tabel.get("rows", [])):
            lokasi = row.get("location")
            indeks.append(
                {
                    "tipe_indeks": "lokasi",
                    "judul": f"Hilal {lokasi} - {bulan_hijriah}",
                    "ringkasan": f"Matahari terbenam {row.get('sunset', {}).get('time')} {row.get('sunset', {}).get('timezone')}",
                    "kata_kunci": ["hilal", lokasi or "", bulan_hijriah],
                    "tanggal_data": None,
                    "jam_ut": None,
                    "nama_lokasi": lokasi,
                    "nomor_halaman_pdf": page,
                    "path_json_pointer": f"/hilal_lokasi/{nomor_tabel}/rows/{nomor_lokasi}",
                    "metadata": {"bulan_hijriah": bulan_hijriah},
                }
            )

    for nomor, page in enumerate(data.get("pages", [])):
        text = page.get("text", "")
        ringkasan = " ".join(text.split())[:220]
        indeks.append(
            {
                "tipe_indeks": "halaman_pdf",
                "judul": f"Halaman PDF {page.get('page')}",
                "ringkasan": ringkasan,
                "kata_kunci": ["halaman pdf", f"halaman {page.get('page')}"],
                "tanggal_data": None,
                "jam_ut": None,
                "nama_lokasi": None,
                "nomor_halaman_pdf": page.get("page"),
                "path_json_pointer": f"/halaman_pdf/{nomor}",
                "metadata": {"jenis_data": "teks_halaman_pdf"},
            }
        )

    return indeks


def build_manifest(
    *,
    kode: str,
    tahun: int,
    versi: str,
    storage_prefix: str,
    source_data: dict[str, Any],
    berkas: list[dict[str, Any]],
    indeks_count: int,
) -> dict[str, Any]:
    dates = [item["date"] for item in source_data.get("ephemeris_daily_blocks", [])]
    return {
        "schema_version": 1,
        "kode": kode,
        "judul": f"Ephemeris Hisab Rukyat Kemenag {tahun}",
        "tahun": tahun,
        "versi": versi,
        "jenis_sumber": "kemenag",
        "sumber_resmi": "SIMBI Kementerian Agama Republik Indonesia",
        "bucket": BUCKET,
        "storage_prefix": storage_prefix,
        "dibuat_pada": datetime.now(timezone.utc).isoformat(),
        "zona_waktu_data": "UT",
        "rentang_tanggal": {
            "mulai": min(dates) if dates else None,
            "selesai": max(dates) if dates else None,
        },
        "jumlah": {
            "halaman_pdf": len(source_data.get("pages", [])),
            "hari_ephemeris": len(source_data.get("ephemeris_daily_blocks", [])),
            "tabel_hilal": len(source_data.get("hilal_location_tables", [])),
            "baris_indeks": indeks_count,
        },
        "checksum_sumber": {
            "pdf_sha256": source_data.get("source", {}).get("pdf_sha256"),
            "layout_text_sha256": source_data.get("extraction", {}).get("layout_text_sha256"),
            "raw_text_sha256": source_data.get("extraction", {}).get("raw_text_sha256"),
        },
        "berkas": berkas,
        "catatan": [
            "Semua teks halaman PDF disimpan di halaman-pdf.json.gz.",
            "Data terstruktur dipisah agar cache Android dapat mengunduh bertahap.",
            "Nilai raw dari PDF tetap dipertahankan pada objek data terkait.",
        ],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("json_kemenag", type=Path)
    parser.add_argument("pdf_kemenag", type=Path)
    parser.add_argument("output_dir", type=Path)
    parser.add_argument("--tahun", type=int, default=2026)
    parser.add_argument("--versi", default="2026.1")
    parser.add_argument("--kode", default="kemenag-2026")
    parser.add_argument("--pretty", action="store_true")
    args = parser.parse_args()

    data = json.loads(args.json_kemenag.read_text(encoding="utf-8"))
    storage_prefix = f"kemenag/{args.tahun}"
    args.output_dir.mkdir(parents=True, exist_ok=True)

    ephemeris_harian = data.get("ephemeris_daily_blocks", [])
    hilal_lokasi = data.get("hilal_location_tables", [])
    halaman_pdf = data.get("pages", [])
    indeks = build_indeks(data)
    metadata_sumber = {
        "schema_version": data.get("schema_version"),
        "source": data.get("source"),
        "extraction": data.get("extraction"),
        "table_of_contents": data.get("table_of_contents", []),
        "sections": data.get("sections", []),
    }

    paths = {
        "metadata": args.output_dir / "metadata-sumber.json",
        "ephemeris": args.output_dir / "ephemeris-harian.json",
        "hilal": args.output_dir / "hilal-lokasi.json",
        "halaman": args.output_dir / "halaman-pdf.json.gz",
        "indeks": args.output_dir / "indeks-pencarian.json",
        "pdf": args.output_dir / args.pdf_kemenag.name,
        "manifest": args.output_dir / "manifest.json",
        "database": args.output_dir / "metadata-database.json",
    }

    write_json(paths["metadata"], metadata_sumber, args.pretty)
    write_json(paths["ephemeris"], {"ephemeris_harian": ephemeris_harian}, args.pretty)
    write_json(paths["hilal"], {"hilal_lokasi": hilal_lokasi}, args.pretty)
    write_json_gzip(paths["halaman"], {"halaman_pdf": halaman_pdf}, args.pretty)
    write_json(paths["indeks"], {"indeks_pencarian": indeks}, args.pretty)
    shutil.copyfile(args.pdf_kemenag, paths["pdf"])

    berkas = [
        file_info(paths["metadata"], "lainnya", "Metadata Sumber", False, 10, 1),
        file_info(paths["ephemeris"], "ephemeris_harian", "Ephemeris Harian", True, 20, len(ephemeris_harian)),
        file_info(paths["hilal"], "hilal_lokasi", "Hilal Lokasi", True, 30, len(hilal_lokasi)),
        file_info(paths["halaman"], "halaman_pdf", "Teks Halaman PDF", False, 40, len(halaman_pdf)),
        file_info(paths["indeks"], "indeks_pencarian", "Indeks Pencarian", True, 50, len(indeks)),
        file_info(paths["pdf"], "pdf_sumber", "PDF Resmi Kemenag", False, 60, 1),
    ]
    for item in berkas:
        item["path_storage"] = f"{storage_prefix}/{item['nama_berkas']}"

    manifest = build_manifest(
        kode=args.kode,
        tahun=args.tahun,
        versi=args.versi,
        storage_prefix=storage_prefix,
        source_data=data,
        berkas=berkas,
        indeks_count=len(indeks),
    )
    write_json(paths["manifest"], manifest, args.pretty)
    manifest_sha = sha256_file(paths["manifest"])
    manifest_info = file_info(paths["manifest"], "manifest", "Manifest Paket", True, 0, 1)
    manifest_info["path_storage"] = f"{storage_prefix}/manifest.json"
    all_berkas = [manifest_info, *berkas]

    dates = [item["date"] for item in ephemeris_harian]
    database_metadata = {
        "paket": {
            "kode": args.kode,
            "judul": f"Ephemeris Hisab Rukyat Kemenag {args.tahun}",
            "deskripsi": "Paket data Ephemeris Kemenag yang diproses dari PDF resmi SIMBI untuk pembelajaran dan praktik Falak.",
            "tahun": args.tahun,
            "versi": args.versi,
            "jenis_sumber": "kemenag",
            "sumber_resmi": "SIMBI Kementerian Agama Republik Indonesia",
            "tautan_sumber": None,
            "bahasa": "id",
            "zona_waktu_data": "UT",
            "status": "aktif",
            "path_manifest_storage": f"{storage_prefix}/manifest.json",
            "sha256_pdf": data.get("source", {}).get("pdf_sha256"),
            "sha256_manifest": manifest_sha,
            "ukuran_total_bytes": sum(item["ukuran_bytes"] for item in all_berkas),
            "jumlah_halaman": len(halaman_pdf),
            "jumlah_hari_ephemeris": len(ephemeris_harian),
            "jumlah_tabel_hilal": len(hilal_lokasi),
            "jumlah_baris_indeks": len(indeks),
            "tanggal_mulai": min(dates) if dates else None,
            "tanggal_selesai": max(dates) if dates else None,
            "catatan_pembaruan": "Data awal dari PDF resmi Kemenag 2026.",
            "metadata": {
                "storage_prefix": storage_prefix,
                "format_cache_android": "manifest + berkas json terpisah",
            },
        },
        "berkas": all_berkas,
        "indeks": indeks,
    }
    write_json(paths["database"], database_metadata, args.pretty)

    print(f"Paket siap: {args.output_dir}")
    print(f"Manifest: {paths['manifest']}")
    print(f"Metadata database: {paths['database']}")
    print(f"Berkas: {len(all_berkas)}")
    print(f"Indeks: {len(indeks)}")


if __name__ == "__main__":
    main()
