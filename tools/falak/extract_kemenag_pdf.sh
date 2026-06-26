#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <kemenag-ephemeris.pdf> [output-dir]" >&2
  exit 2
fi

pdf_path="$1"
out_dir="${2:-build/falak/kemenag}"

if [[ ! -f "$pdf_path" ]]; then
  echo "PDF not found: $pdf_path" >&2
  exit 1
fi

for bin in pdfinfo pdftotext; do
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "Missing required command: $bin" >&2
    echo "Install it on Arch Linux with: sudo pacman -S poppler" >&2
    exit 1
  fi
done

mkdir -p "$out_dir/pages"

pdfinfo "$pdf_path" > "$out_dir/pdfinfo.txt"
pages="$(awk -F: '/^Pages:/ { gsub(/^[ \t]+/, "", $2); print $2 }' "$out_dir/pdfinfo.txt")"

if [[ -z "$pages" ]]; then
  echo "Unable to determine PDF page count." >&2
  exit 1
fi

pdftotext -layout "$pdf_path" "$out_dir/layout.txt"
pdftotext -raw "$pdf_path" "$out_dir/raw.txt"

page=1
while [[ "$page" -le "$pages" ]]; do
  page_file="$out_dir/pages/page-$(printf "%03d" "$page").txt"
  pdftotext -layout -f "$page" -l "$page" "$pdf_path" "$page_file"
  page=$((page + 1))
done

cat > "$out_dir/source.json" <<EOF
{
  "source_pdf": "$pdf_path",
  "pages": $pages,
  "layout_text": "$out_dir/layout.txt",
  "raw_text": "$out_dir/raw.txt",
  "page_dir": "$out_dir/pages"
}
EOF

echo "Extracted $pages pages into $out_dir"
