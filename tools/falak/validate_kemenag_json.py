#!/usr/bin/env python3
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
from datetime import date, timedelta
from pathlib import Path
from typing import Any


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def parse_pdfinfo_pages(path: Path) -> int | None:
    if not path.exists():
        return None
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("Pages:"):
            return int(line.split(":", 1)[1].strip())
    return None


def iso_dates_between(start: str, end: str) -> list[str]:
    current = date.fromisoformat(start)
    finish = date.fromisoformat(end)
    result: list[str] = []
    while current <= finish:
        result.append(current.isoformat())
        current += timedelta(days=1)
    return result


def validate_pages(
    data: dict[str, Any],
    *,
    min_pages: int,
    extracted_dir: Path | None,
    errors: list[str],
    warnings: list[str],
) -> None:
    pages = data.get("pages", [])
    extraction = data.get("extraction", {})
    page_count = extraction.get("page_count")
    if len(pages) < min_pages:
        errors.append(f"Jumlah pages di bawah ambang: {len(pages)} < {min_pages}")
    if page_count is not None and page_count != len(pages):
        errors.append(f"extraction.page_count tidak sama dengan pages[]: {page_count} != {len(pages)}")
    page_numbers = [item.get("page") for item in pages]
    expected = list(range(1, len(pages) + 1))
    if page_numbers != expected:
        errors.append("Nomor halaman pages[] tidak berurutan mulai dari 1.")
    empty_pages = [item.get("page") for item in pages if not str(item.get("text", "")).strip()]
    if empty_pages:
        warnings.append(f"Ada halaman hasil ekstraksi kosong: {empty_pages[:12]}")

    if extracted_dir:
        pdfinfo_pages = parse_pdfinfo_pages(extracted_dir / "pdfinfo.txt")
        if pdfinfo_pages is not None and pdfinfo_pages != len(pages):
            errors.append(f"Jumlah halaman pdfinfo tidak sama dengan JSON: {pdfinfo_pages} != {len(pages)}")
        page_files = sorted((extracted_dir / "pages").glob("page-*.txt"))
        if page_files and len(page_files) != len(pages):
            errors.append(f"Jumlah file page-*.txt tidak sama dengan JSON: {len(page_files)} != {len(pages)}")
        layout = extracted_dir / "layout.txt"
        raw = extracted_dir / "raw.txt"
        if layout.exists() and extraction.get("layout_text_sha256") != sha256_file(layout):
            errors.append("Checksum layout.txt tidak sama dengan extraction.layout_text_sha256.")
        if raw.exists() and extraction.get("raw_text_sha256") != sha256_file(raw):
            errors.append("Checksum raw.txt tidak sama dengan extraction.raw_text_sha256.")


def validate_pdf_hash(data: dict[str, Any], pdf: Path | None, errors: list[str]) -> None:
    if not pdf:
        return
    expected = data.get("source", {}).get("pdf_sha256")
    actual = sha256_file(pdf)
    if expected != actual:
        errors.append(f"Checksum PDF tidak sama: JSON={expected}, file={actual}")


def validate_hilal_tables(data: dict[str, Any], *, min_hilal_tables: int, errors: list[str], warnings: list[str]) -> None:
    tables = data.get("hilal_location_tables", [])
    if len(tables) < min_hilal_tables:
        errors.append(f"Jumlah tabel hilal di bawah ambang: {len(tables)} < {min_hilal_tables}")
    for index, table in enumerate(tables, start=1):
        rows = table.get("rows", [])
        if not rows:
            warnings.append(f"Tabel hilal ke-{index} halaman {table.get('page')} tidak memiliki rows terstruktur.")
            continue
        numbers = [row.get("no") for row in rows]
        if numbers != list(range(1, len(rows) + 1)):
            warnings.append(f"Nomor lokasi tabel hilal ke-{index} tidak berurutan penuh.")
        for row in rows:
            for key in ("sunset", "moonset"):
                value = row.get(key, {})
                if not value.get("time") or not value.get("timezone"):
                    errors.append(f"Tabel hilal ke-{index} lokasi {row.get('location')} tidak punya {key} lengkap.")


def validate_ephemeris_days(
    data: dict[str, Any],
    *,
    min_ephemeris_days: int,
    expected_year: int | None,
    strict_hourly: bool,
    errors: list[str],
    warnings: list[str],
) -> None:
    blocks = data.get("ephemeris_daily_blocks", [])
    if len(blocks) < min_ephemeris_days:
        errors.append(f"Jumlah blok ephemeris harian di bawah ambang: {len(blocks)} < {min_ephemeris_days}")
    dates = [item.get("date") for item in blocks]
    if len(dates) != len(set(dates)):
        errors.append("Tanggal ephemeris_daily_blocks mengandung duplikasi.")
    sorted_dates = sorted(dates)
    if dates != sorted_dates:
        warnings.append("Tanggal ephemeris_daily_blocks tidak tersimpan berurutan naik.")
    if expected_year:
        expected_dates = iso_dates_between(f"{expected_year}-01-01", f"{expected_year}-12-31")
        missing = sorted(set(expected_dates) - set(dates))
        extra = sorted(date_text for date_text in dates if not str(date_text).startswith(f"{expected_year}-"))
        if missing:
            errors.append(f"Tanggal ephemeris tahun {expected_year} tidak lengkap. Hilang {len(missing)} tanggal, contoh: {missing[:10]}")
        if extra:
            warnings.append(f"Ada tanggal di luar tahun {expected_year}: {extra[:10]}")

    for block in blocks:
        if not block.get("has_structured_hourly_table"):
            warnings.append(f"Ephemeris {block.get('date')} tidak memiliki tabel jam terstruktur.")
            continue
        table = block.get("hourly_table", {})
        for name in ("sun", "moon"):
            rows = table.get(name, [])
            hours = [row.get("hour_ut") for row in rows]
            expected_hours = list(range(0, 25))
            if strict_hourly and hours != expected_hours:
                errors.append(f"Ephemeris {block.get('date')} tabel {name} jam tidak lengkap 00-24: {hours}")
            elif len(rows) != 25:
                warnings.append(f"Ephemeris {block.get('date')} tabel {name} memiliki {len(rows)} baris, bukan 25.")


def validate_package(package_dir: Path | None, data: dict[str, Any], errors: list[str], warnings: list[str]) -> None:
    if not package_dir:
        return
    manifest_path = package_dir / "manifest.json"
    database_path = package_dir / "metadata-database.json"
    if not manifest_path.exists():
        errors.append(f"manifest.json tidak ditemukan di {package_dir}")
        return
    manifest = read_json(manifest_path)
    required = [
        "metadata-sumber.json",
        "ephemeris-harian.json",
        "hilal-lokasi.json",
        "halaman-pdf.json.gz",
        "indeks-pencarian.json",
    ]
    for name in required:
        if not (package_dir / name).exists():
            errors.append(f"Berkas paket wajib tidak ditemukan: {name}")

    for item in manifest.get("berkas", []):
        path = package_dir / item.get("nama_berkas", "")
        if not path.exists():
            errors.append(f"Berkas manifest tidak ada di paket: {item.get('nama_berkas')}")
            continue
        if item.get("sha256") != sha256_file(path):
            errors.append(f"Checksum berkas paket tidak sama: {item.get('nama_berkas')}")
        if item.get("ukuran_bytes") != path.stat().st_size:
            errors.append(f"Ukuran berkas paket tidak sama: {item.get('nama_berkas')}")

    ephemeris_path = package_dir / "ephemeris-harian.json"
    if ephemeris_path.exists():
        packaged = read_json(ephemeris_path).get("ephemeris_harian", [])
        if len(packaged) != len(data.get("ephemeris_daily_blocks", [])):
            errors.append("Jumlah ephemeris-harian.json tidak sama dengan JSON sumber.")
    hilal_path = package_dir / "hilal-lokasi.json"
    if hilal_path.exists():
        packaged = read_json(hilal_path).get("hilal_lokasi", [])
        if len(packaged) != len(data.get("hilal_location_tables", [])):
            errors.append("Jumlah hilal-lokasi.json tidak sama dengan JSON sumber.")
    halaman_path = package_dir / "halaman-pdf.json.gz"
    if halaman_path.exists():
        with gzip.open(halaman_path, "rt", encoding="utf-8") as handle:
            packaged = json.load(handle).get("halaman_pdf", [])
        if len(packaged) != len(data.get("pages", [])):
            errors.append("Jumlah halaman-pdf.json.gz tidak sama dengan JSON sumber.")

    if database_path.exists():
        database = read_json(database_path)
        paket = database.get("paket", {})
        jumlah = manifest.get("jumlah", {})
        if paket.get("jumlah_halaman") != jumlah.get("halaman_pdf"):
            errors.append("metadata-database jumlah_halaman tidak sama dengan manifest.")
        if paket.get("jumlah_hari_ephemeris") != jumlah.get("hari_ephemeris"):
            errors.append("metadata-database jumlah_hari_ephemeris tidak sama dengan manifest.")
    else:
        warnings.append("metadata-database.json belum ada; validasi database dilewati.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Validasi JSON dan paket Ephemeris Kemenag.")
    parser.add_argument("json_path", type=Path)
    parser.add_argument("--pdf", type=Path)
    parser.add_argument("--extracted-dir", type=Path)
    parser.add_argument("--package-dir", type=Path)
    parser.add_argument("--tahun", type=int)
    parser.add_argument("--min-pages", type=int, default=400)
    parser.add_argument("--min-hilal-tables", type=int, default=10)
    parser.add_argument("--min-ephemeris-days", type=int, default=300)
    parser.add_argument("--strict-hourly", action="store_true", help="Wajibkan tabel Matahari/Bulan 00-24 lengkap untuk setiap hari.")
    parser.add_argument("--report-json", type=Path, help="Tulis laporan validasi ke file JSON.")
    args = parser.parse_args()

    data = read_json(args.json_path)
    errors: list[str] = []
    warnings: list[str] = []

    validate_pdf_hash(data, args.pdf, errors)
    validate_pages(data, min_pages=args.min_pages, extracted_dir=args.extracted_dir, errors=errors, warnings=warnings)
    validate_hilal_tables(data, min_hilal_tables=args.min_hilal_tables, errors=errors, warnings=warnings)
    validate_ephemeris_days(
        data,
        min_ephemeris_days=args.min_ephemeris_days,
        expected_year=args.tahun,
        strict_hourly=args.strict_hourly,
        errors=errors,
        warnings=warnings,
    )
    validate_package(args.package_dir, data, errors, warnings)

    summary = {
        "status": "error" if errors else "ok",
        "json_path": str(args.json_path),
        "pdf": str(args.pdf) if args.pdf else None,
        "extracted_dir": str(args.extracted_dir) if args.extracted_dir else None,
        "package_dir": str(args.package_dir) if args.package_dir else None,
        "counts": {
            "pages": len(data.get("pages", [])),
            "hilal_location_tables": len(data.get("hilal_location_tables", [])),
            "ephemeris_daily_blocks": len(data.get("ephemeris_daily_blocks", [])),
            "sections": len(data.get("sections", [])),
            "table_of_contents": len(data.get("table_of_contents", [])),
        },
        "errors": errors,
        "warnings": warnings,
    }

    if args.report_json:
        args.report_json.parent.mkdir(parents=True, exist_ok=True)
        args.report_json.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    for warning in warnings:
        print(f"WARNING: {warning}")
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        raise SystemExit(1)

    print("OK")
    for key, value in summary["counts"].items():
        print(f"{key}: {value}")


if __name__ == "__main__":
    main()
