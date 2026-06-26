#!/usr/bin/env python3
"""
Command satu pintu untuk mengubah PDF resmi Ephemeris Kemenag menjadi JSON dan
paket data siap Android/Supabase, disertai validasi konsistensi otomatis.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
TOOLS_DIR = Path(__file__).resolve().parent


def run(command: list[str], *, dry_run: bool = False) -> None:
    print("+ " + " ".join(command))
    if dry_run:
        return
    subprocess.run(command, check=True)


def infer_year(pdf_path: Path) -> int:
    match = re.search(r"(20\d{2})", pdf_path.name)
    if match:
        return int(match.group(1))
    raise SystemExit("Tahun tidak ditemukan dari nama PDF. Gunakan --tahun 2026.")


def default_output_root(year: int) -> Path:
    return ROOT / "build" / "falak" / f"kemenag-{year}"


def summarize(json_path: Path, package_dir: Path | None, report_path: Path | None) -> None:
    data = json.loads(json_path.read_text(encoding="utf-8"))
    print()
    print("Ringkasan konversi")
    print(f"JSON: {json_path}")
    print(f"Halaman PDF: {len(data.get('pages', []))}")
    print(f"Hari ephemeris: {len(data.get('ephemeris_daily_blocks', []))}")
    print(f"Tabel hilal: {len(data.get('hilal_location_tables', []))}")
    if package_dir:
        print(f"Paket: {package_dir}")
    if report_path:
        print(f"Laporan validasi: {report_path}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Konversi PDF Ephemeris Kemenag tahunan menjadi JSON, paket cache Android, dan laporan validasi."
    )
    parser.add_argument("pdf", type=Path, help="PDF resmi Kemenag, misalnya referensi/falaq/kemenag-2026.pdf")
    parser.add_argument("--tahun", type=int, help="Tahun ephemeris. Jika kosong diambil dari nama PDF.")
    parser.add_argument("--output-root", type=Path, help="Folder ekstraksi teks. Default build/falak/kemenag-<tahun>.")
    parser.add_argument("--json-output", type=Path, help="Path JSON hasil parse. Default build/falak/kemenag-<tahun>.json.")
    parser.add_argument("--package-dir", type=Path, help="Folder paket output. Default build/falak/paket-kemenag-<tahun>.")
    parser.add_argument("--kode", help="Kode paket. Default kemenag-<tahun>.")
    parser.add_argument("--versi", help="Versi paket. Default <tahun>.1.")
    parser.add_argument("--pretty", action="store_true", help="Tulis JSON dengan indentasi agar mudah dibaca.")
    parser.add_argument("--skip-package", action="store_true", help="Hanya ekstrak, parse, dan validasi JSON; tidak membuat paket Android/Supabase.")
    parser.add_argument("--strict-hourly", action="store_true", help="Wajibkan tabel Matahari/Bulan 00-24 lengkap untuk setiap hari.")
    parser.add_argument("--min-pages", type=int, default=400)
    parser.add_argument("--min-hilal-tables", type=int, default=10)
    parser.add_argument("--min-ephemeris-days", type=int, default=300)
    parser.add_argument("--dry-run", action="store_true", help="Cetak command tanpa menjalankan.")
    args = parser.parse_args()

    pdf = args.pdf.resolve()
    if not pdf.exists():
        raise SystemExit(f"PDF tidak ditemukan: {pdf}")

    year = args.tahun or infer_year(pdf)
    output_root = (args.output_root or default_output_root(year)).resolve()
    json_output = (args.json_output or (ROOT / "build" / "falak" / f"kemenag-{year}.json")).resolve()
    package_dir = None if args.skip_package else (args.package_dir or (ROOT / "build" / "falak" / f"paket-kemenag-{year}")).resolve()
    kode = args.kode or f"kemenag-{year}"
    versi = args.versi or f"{year}.1"
    report_path = output_root / "validasi-konsistensi.json"

    extract_script = TOOLS_DIR / "extract_kemenag_pdf.sh"
    parse_script = TOOLS_DIR / "parse_kemenag_ephemeris.py"
    validate_script = TOOLS_DIR / "validate_kemenag_json.py"
    package_script = TOOLS_DIR / "siapkan_paket_kemenag.py"

    run([str(extract_script), str(pdf), str(output_root)], dry_run=args.dry_run)
    run(
        [
            sys.executable,
            str(parse_script),
            str(pdf),
            str(output_root),
            str(json_output),
            *(["--pretty"] if args.pretty else []),
        ],
        dry_run=args.dry_run,
    )
    run(
        [
            sys.executable,
            str(validate_script),
            str(json_output),
            "--pdf",
            str(pdf),
            "--extracted-dir",
            str(output_root),
            "--tahun",
            str(year),
            "--min-pages",
            str(args.min_pages),
            "--min-hilal-tables",
            str(args.min_hilal_tables),
            "--min-ephemeris-days",
            str(args.min_ephemeris_days),
            "--report-json",
            str(report_path),
            *(["--strict-hourly"] if args.strict_hourly else []),
        ],
        dry_run=args.dry_run,
    )

    if package_dir:
        run(
            [
                sys.executable,
                str(package_script),
                str(json_output),
                str(pdf),
                str(package_dir),
                "--tahun",
                str(year),
                "--versi",
                versi,
                "--kode",
                kode,
                *(["--pretty"] if args.pretty else []),
            ],
            dry_run=args.dry_run,
        )
        run(
            [
                sys.executable,
                str(validate_script),
                str(json_output),
                "--pdf",
                str(pdf),
                "--extracted-dir",
                str(output_root),
                "--package-dir",
                str(package_dir),
                "--tahun",
                str(year),
                "--min-pages",
                str(args.min_pages),
                "--min-hilal-tables",
                str(args.min_hilal_tables),
                "--min-ephemeris-days",
                str(args.min_ephemeris_days),
                "--report-json",
                str(report_path),
                *(["--strict-hourly"] if args.strict_hourly else []),
            ],
            dry_run=args.dry_run,
        )

    if not args.dry_run:
        summarize(json_output, package_dir, report_path)


if __name__ == "__main__":
    main()
