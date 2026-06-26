#!/usr/bin/env python3
"""
Parse Kemenag Ephemeris PDF text extraction into an audit-friendly JSON file.

The parser intentionally preserves every page's extracted text. Structured data
is added incrementally for sections that can be recognized safely.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


MONTHS_ID = {
    "JANUARI": "01",
    "FEBRUARI": "02",
    "MARET": "03",
    "APRIL": "04",
    "MEI": "05",
    "JUNI": "06",
    "JULI": "07",
    "AGUSTUS": "08",
    "SEPTEMBER": "09",
    "OKTOBER": "10",
    "NOVEMBER": "11",
    "DESEMBER": "12",
}

DATE_HEADING_RE = re.compile(
    r"(?P<day>\d{2})\s+(?P<month>"
    + "|".join(MONTHS_ID.keys())
    + r")\s+(?P<year>\d{4})\s+M",
    re.IGNORECASE,
)

HILAL_TABLE_RE = re.compile(
    r"DATA HILAL DAN MATAHARI PADA SAAT MATAHARI TERBENAM\s+"
    r"(?P<event_date>.+?)\s+"
    r"PENENTU AWAL BULAN\s+(?P<hijri_month>.+?)\s+"
    r"IJTIMAK:\s+(?P<ijtima>.+?)(?:\n|$)",
    re.DOTALL,
)

LOCATION_ROW_RE = re.compile(
    r"^\s*(?P<no>\d{1,2})\s+"
    r"(?P<location>[A-Za-zÀ-ÿ .'-]+?)\s+"
    r"(?P<sunset>\d{2}\.\d{2}\.\d{2})\s+"
    r"(?P<sunset_tz>WIB|WITA|WIT)\s+"
    r"(?P<moonset>\d{2}\.\d{2}\.\d{2})\s+"
    r"(?P<moonset_tz>WIB|WITA|WIT)\s+"
    r"(?P<rest>.+?)\s*$"
)

NUM_TOKEN_RE = re.compile(r"-?\d+(?:[,.]\d+)?")
TIME_ROW_RE = re.compile(r"^(?P<hour>\d{2}):00\s+(?P<rest>.+)$")
DMS_PATTERN = r"(?P<sign>[+-]?)(?P<deg>\d+)°\s+(?P<min>\d+)'\s+(?P<sec>\d+(?:,\d+)?)\""
SIGNED_DMS_RE = re.compile(r"^" + DMS_PATTERN + r"$")
SUN_ROW_RE = re.compile(
    r"^(?P<hour>\d{2}:00)\s+"
    r"(?P<apparent_ecliptic_longitude>[+-]?\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<apparent_ecliptic_latitude>[+-]?\d+(?:,\d+)?\")\s+"
    r"(?P<apparent_right_ascension>[+-]?\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<apparent_declination>[+-]?\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<true_geocentric_distance>\d+,\d+)\s+"
    r"(?P<semi_diameter>\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<true_obliquity>\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<equation_of_time>[+-]?\d+m\s+\d+s)\s*$"
)
MOON_ROW_RE = re.compile(
    r"^(?P<hour>\d{2}:00)\s+"
    r"(?P<apparent_longitude>[+-]?\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<apparent_latitude>[+-]?\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<apparent_right_ascension>[+-]?\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<apparent_declination>[+-]?\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<horizontal_parallax>\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<semi_diameter>\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<angle_bright_limb>\d+°\s+\d+'\s+\d+(?:,\d+)?\")\s+"
    r"(?P<fraction_illumination>\d+(?:,\d+)?)%\s*$"
)


@dataclass
class Page:
    page: int
    text: str


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_pages(page_dir: Path) -> list[Page]:
    pages: list[Page] = []
    for path in sorted(page_dir.glob("page-*.txt")):
        match = re.search(r"page-(\d+)\.txt$", path.name)
        if not match:
            continue
        pages.append(Page(page=int(match.group(1)), text=path.read_text(encoding="utf-8", errors="replace")))
    return pages


def clean_line(line: str) -> str:
    return re.sub(r"\s+", " ", line).strip()


def parse_pdfinfo(text: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in text.splitlines():
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        result[key.strip()] = value.strip()
    return result


def parse_toc(layout_text: str) -> list[dict[str, Any]]:
    toc: list[dict[str, Any]] = []
    in_toc = False
    for line in layout_text.splitlines():
        stripped = clean_line(line)
        if stripped == "DAFTAR ISI":
            in_toc = True
            continue
        if in_toc and stripped.startswith("PENJELASAN DATA EPHEMERIS"):
            break
        if not in_toc or not stripped:
            continue
        match = re.match(r"^(?P<title>.+?)\s+\.{2,}\s*(?P<page>\d+)$", stripped)
        if match:
            toc.append({"title": match.group("title").strip(), "printed_page": int(match.group("page"))})
            continue
        match = re.match(r"^(?P<title>.+?)\s+(?P<page>\d+)$", stripped)
        if match and len(match.group("title")) > 5:
            toc.append({"title": match.group("title").strip(), "printed_page": int(match.group("page"))})
    return toc


def parse_decimal_token(token: str) -> float:
    return float(token.replace(",", "."))


def parse_dms(value: str) -> dict[str, Any]:
    match = SIGNED_DMS_RE.match(value.strip())
    if not match:
        return {"raw": value}
    sign = -1.0 if match.group("sign") == "-" else 1.0
    degree = int(match.group("deg"))
    minute = int(match.group("min"))
    second = parse_decimal_token(match.group("sec"))
    decimal = sign * (degree + minute / 60.0 + second / 3600.0)
    return {
        "raw": value,
        "degree": sign * degree,
        "minute": minute,
        "second": second,
        "decimal_degree": decimal,
    }


def parse_arcsecond(value: str) -> dict[str, Any]:
    raw = value.strip()
    number = parse_decimal_token(raw.replace('"', ""))
    return {
        "raw": value,
        "arcsecond": number,
        "decimal_degree": number / 3600.0,
    }


def parse_minute_second(value: str) -> dict[str, Any]:
    raw = value.strip()
    match = re.match(r"^(?P<sign>[+-]?)(?P<minute>\d+)'\s+(?P<second>\d+(?:,\d+)?)\"$", raw)
    if not match:
        return {"raw": value}
    sign = -1.0 if match.group("sign") == "-" else 1.0
    minute = int(match.group("minute"))
    second = parse_decimal_token(match.group("second"))
    return {
        "raw": value,
        "arcminute": sign * minute,
        "arcsecond": second,
        "decimal_degree": sign * (minute / 60.0 + second / 3600.0),
    }


def parse_equation_of_time(value: str) -> dict[str, Any]:
    raw = value.strip()
    match = re.match(r"^(?P<sign>[+-]?)(?P<minute>\d+)m\s+(?P<second>\d+)s$", raw)
    if not match:
        return {"raw": value}
    sign = -1.0 if match.group("sign") == "-" else 1.0
    minute = int(match.group("minute"))
    second = int(match.group("second"))
    total_seconds = sign * (minute * 60 + second)
    return {
        "raw": value,
        "minute": sign * minute,
        "second": second,
        "total_seconds": total_seconds,
        "hours": total_seconds / 3600.0,
    }


def parse_hour(hour_label: str) -> int:
    return int(hour_label.split(":", 1)[0])


def split_angle_pairs(tokens: list[float]) -> dict[str, Any]:
    # Rows contain degree/minute pairs after time columns:
    # sun az deg/min, moon az deg/min, moon alt deg/min, elongation deg/min, FI.
    fields = [
        "sun_azimuth",
        "moon_azimuth",
        "moon_altitude",
        "elongation",
    ]
    parsed: dict[str, Any] = {}
    index = 0
    for field in fields:
        if index + 1 >= len(tokens):
            break
        degree = tokens[index]
        minute = tokens[index + 1]
        sign = -1.0 if degree < 0 else 1.0
        parsed[field] = {
            "degree": degree,
            "minute": minute,
            "decimal_degree": degree + (sign * minute / 60.0),
        }
        index += 2
    if index < len(tokens):
        parsed["fraction_illumination"] = tokens[index]
    return parsed


def parse_hilal_location_rows(text: str) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    pending = ""
    for raw_line in text.splitlines():
        line = clean_line(raw_line)
        if not line:
            continue
        candidate = f"{pending} {line}".strip() if pending else line
        match = LOCATION_ROW_RE.match(candidate)
        if not match:
            if re.match(r"^\d{1,2}\s+[A-Za-zÀ-ÿ .'-]+", line):
                pending = line
            elif pending:
                pending = candidate
            continue
        numbers = [parse_decimal_token(item) for item in NUM_TOKEN_RE.findall(match.group("rest"))]
        row = {
            "no": int(match.group("no")),
            "location": clean_line(match.group("location")),
            "sunset": {"time": match.group("sunset").replace(".", ":"), "timezone": match.group("sunset_tz")},
            "moonset": {"time": match.group("moonset").replace(".", ":"), "timezone": match.group("moonset_tz")},
            "raw_tail": match.group("rest"),
            **split_angle_pairs(numbers),
        }
        rows.append(row)
        pending = ""
    return rows


def parse_hilal_tables(pages: list[Page]) -> list[dict[str, Any]]:
    tables: list[dict[str, Any]] = []
    for page in pages:
        if "DATA HILAL DAN MATAHARI PADA SAAT MATAHARI TERBENAM" not in page.text:
            continue
        header = HILAL_TABLE_RE.search(page.text)
        rows = parse_hilal_location_rows(page.text)
        tables.append(
            {
                "page": page.page,
                "event_date_raw": clean_line(header.group("event_date")) if header else None,
                "hijri_month_raw": clean_line(header.group("hijri_month")) if header else None,
                "ijtima_raw": clean_line(header.group("ijtima")) if header else None,
                "rows": rows,
                "raw_text": page.text,
            }
        )
    return tables


def page_date(page: Page) -> str | None:
    match = DATE_HEADING_RE.search(page.text)
    if not match:
        return None
    month = MONTHS_ID[match.group("month").upper()]
    return f"{match.group('year')}-{month}-{match.group('day')}"


def parse_sun_row(line: str) -> dict[str, Any] | None:
    match = SUN_ROW_RE.match(clean_line(line))
    if not match:
        return None
    return {
        "hour_label": match.group("hour"),
        "hour_ut": parse_hour(match.group("hour")),
        "apparent_ecliptic_longitude": parse_dms(match.group("apparent_ecliptic_longitude")),
        "apparent_ecliptic_latitude": parse_arcsecond(match.group("apparent_ecliptic_latitude")),
        "apparent_right_ascension": parse_dms(match.group("apparent_right_ascension")),
        "apparent_declination": parse_dms(match.group("apparent_declination")),
        "true_geocentric_distance_au": parse_decimal_token(match.group("true_geocentric_distance")),
        "semi_diameter": parse_minute_second(match.group("semi_diameter")),
        "true_obliquity": parse_dms(match.group("true_obliquity")),
        "equation_of_time": parse_equation_of_time(match.group("equation_of_time")),
        "raw": clean_line(line),
    }


def parse_moon_row(line: str) -> dict[str, Any] | None:
    match = MOON_ROW_RE.match(clean_line(line))
    if not match:
        return None
    return {
        "hour_label": match.group("hour"),
        "hour_ut": parse_hour(match.group("hour")),
        "apparent_longitude": parse_dms(match.group("apparent_longitude")),
        "apparent_latitude": parse_dms(match.group("apparent_latitude")),
        "apparent_right_ascension": parse_dms(match.group("apparent_right_ascension")),
        "apparent_declination": parse_dms(match.group("apparent_declination")),
        "horizontal_parallax": parse_minute_second(match.group("horizontal_parallax")),
        "semi_diameter": parse_minute_second(match.group("semi_diameter")),
        "angle_bright_limb": parse_dms(match.group("angle_bright_limb")),
        "fraction_illumination_percent": parse_decimal_token(match.group("fraction_illumination")),
        "raw": clean_line(line),
    }


def parse_daily_hourly_table(text: str) -> dict[str, Any]:
    sun_rows: list[dict[str, Any]] = []
    moon_rows: list[dict[str, Any]] = []
    table = None
    for line in text.splitlines():
        clean = clean_line(line)
        if clean == "DATA MATAHARI":
            table = "sun"
            continue
        if clean == "DATA BULAN":
            table = "moon"
            continue
        if not TIME_ROW_RE.match(clean):
            continue
        if table == "sun":
            parsed = parse_sun_row(clean)
            if parsed:
                sun_rows.append(parsed)
        elif table == "moon":
            parsed = parse_moon_row(clean)
            if parsed:
                moon_rows.append(parsed)
    return {
        "sun": sun_rows,
        "moon": moon_rows,
    }


def parse_ephemeris_day_pages(pages: list[Page]) -> list[dict[str, Any]]:
    blocks: list[dict[str, Any]] = []
    for page in pages:
        iso_date = page_date(page)
        if not iso_date:
            continue
        if "Apparent Right" not in page.text and "Equation of" not in page.text:
            continue
        hourly_table = parse_daily_hourly_table(page.text)
        blocks.append(
            {
                "page": page.page,
                "date": iso_date,
                "has_structured_hourly_table": bool(hourly_table["sun"] or hourly_table["moon"]),
                "hourly_table": hourly_table,
                "raw_text": page.text,
            }
        )
    return blocks


def parse_sections(pages: list[Page]) -> list[dict[str, Any]]:
    heading_patterns = [
        r"^PENJELASAN DATA EPHEMERIS",
        r"^DAFTAR GERHANA",
        r"^DAFTAR WAKTU IJTIMAK",
        r"^DATA HILAL DAN MATAHARI",
        r"^DATA POSISI MATAHARI DAN BULAN",
        r"^LAMPIRAN",
    ]
    sections: list[dict[str, Any]] = []
    for page in pages:
        for line in page.text.splitlines():
            title = clean_line(line)
            if any(re.search(pattern, title, re.IGNORECASE) for pattern in heading_patterns):
                sections.append({"page": page.page, "title": title})
                break
    return sections


def build_json(pdf_path: Path, extracted_dir: Path) -> dict[str, Any]:
    layout_path = extracted_dir / "layout.txt"
    raw_path = extracted_dir / "raw.txt"
    pdfinfo_path = extracted_dir / "pdfinfo.txt"
    page_dir = extracted_dir / "pages"

    layout_text = layout_path.read_text(encoding="utf-8", errors="replace")
    pages = read_pages(page_dir)

    return {
        "schema_version": 1,
        "source": {
            "name": "Ephemeris Hisab Rukyat Kemenag",
            "pdf_path": str(pdf_path),
            "pdf_sha256": sha256_file(pdf_path),
            "extracted_at": datetime.now(timezone.utc).isoformat(),
            "pdfinfo": parse_pdfinfo(pdfinfo_path.read_text(encoding="utf-8", errors="replace")),
        },
        "extraction": {
            "tool": "pdftotext -layout",
            "layout_text_sha256": sha256_file(layout_path),
            "raw_text_sha256": sha256_file(raw_path),
            "page_count": len(pages),
            "notes": [
                "Every extracted page is preserved under pages[].text.",
                "Structured tables are added only when safely recognized.",
            ],
        },
        "table_of_contents": parse_toc(layout_text),
        "sections": parse_sections(pages),
        "hilal_location_tables": parse_hilal_tables(pages),
        "ephemeris_daily_blocks": parse_ephemeris_day_pages(pages),
        "pages": [{"page": page.page, "text": page.text} for page in pages],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("extracted_dir", type=Path)
    parser.add_argument("output_json", type=Path)
    parser.add_argument("--pretty", action="store_true")
    args = parser.parse_args()

    data = build_json(args.pdf, args.extracted_dir)
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    with args.output_json.open("w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2 if args.pretty else None)
        handle.write("\n")

    print(
        "Wrote "
        f"{args.output_json} with {len(data['pages'])} pages, "
        f"{len(data['hilal_location_tables'])} hilal tables, "
        f"{len(data['ephemeris_daily_blocks'])} ephemeris day blocks."
    )


if __name__ == "__main__":
    main()
