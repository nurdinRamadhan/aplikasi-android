# Kemenag Ephemeris PDF Pipeline

Pipeline ini mengubah PDF resmi Ephemeris Hisab Rukyat Kemenag menjadi JSON
yang tetap audit-friendly. Semua teks halaman PDF disimpan di `pages[]`, lalu
bagian yang dapat dikenali otomatis ditambahkan sebagai struktur tambahan.

## Prasyarat Arch Linux

```bash
sudo pacman -S poppler jq
```

`poppler` menyediakan `pdfinfo` dan `pdftotext`.

## Command Satu Pintu

Untuk PDF Kemenag tahunan baru, gunakan script terpadu berikut:

```bash
tools/falak/konversi_kemenag_pdf.py \
  referensi/falaq/kemenag-2026.pdf \
  --tahun 2026
```

Output default:

- `build/falak/kemenag-2026/`: hasil ekstraksi teks PDF.
- `build/falak/kemenag-2026.json`: JSON audit lengkap.
- `build/falak/paket-kemenag-2026/`: paket cache Android/Supabase.
- `build/falak/kemenag-2026/validasi-konsistensi.json`: laporan validasi otomatis.

Untuk JSON yang mudah dibaca:

```bash
tools/falak/konversi_kemenag_pdf.py \
  referensi/falaq/kemenag-2026.pdf \
  --tahun 2026 \
  --pretty
```

Untuk hanya membuat JSON tanpa paket Android/Supabase:

```bash
tools/falak/konversi_kemenag_pdf.py \
  referensi/falaq/kemenag-2026.pdf \
  --tahun 2026 \
  --skip-package
```

Jika ingin audit ketat bahwa setiap tabel harian memiliki baris Matahari dan
Bulan jam `00` sampai `24`:

```bash
tools/falak/konversi_kemenag_pdf.py \
  referensi/falaq/kemenag-2026.pdf \
  --tahun 2026 \
  --strict-hourly
```

Catatan: `--strict-hourly` cocok setelah parser sudah stabil untuk format PDF
tahun tersebut. Untuk PDF dengan layout baru, jalankan tanpa opsi ini dahulu,
lalu periksa warning di laporan validasi.

## Ekstrak PDF

```bash
tools/falak/extract_kemenag_pdf.sh \
  referensi/falaq/kemenag-2026.pdf \
  build/falak/kemenag-2026
```

Output:

- `layout.txt`: teks seluruh PDF dengan layout dipertahankan.
- `raw.txt`: teks seluruh PDF dalam mode raw.
- `pages/page-001.txt` dan seterusnya: teks per halaman.
- `pdfinfo.txt`: metadata PDF.

## Parse ke JSON

```bash
tools/falak/parse_kemenag_ephemeris.py \
  referensi/falaq/kemenag-2026.pdf \
  build/falak/kemenag-2026 \
  build/falak/kemenag-2026.json
```

Untuk JSON mudah dibaca:

```bash
tools/falak/parse_kemenag_ephemeris.py \
  referensi/falaq/kemenag-2026.pdf \
  build/falak/kemenag-2026 \
  build/falak/kemenag-2026.pretty.json \
  --pretty
```

## Validasi Dasar

```bash
tools/falak/validate_kemenag_json.py build/falak/kemenag-2026.json
```

Validasi lengkap terhadap PDF, hasil ekstraksi, dan paket:

```bash
tools/falak/validate_kemenag_json.py \
  build/falak/kemenag-2026.json \
  --pdf referensi/falaq/kemenag-2026.pdf \
  --extracted-dir build/falak/kemenag-2026 \
  --package-dir build/falak/paket-kemenag-2026 \
  --tahun 2026 \
  --report-json build/falak/kemenag-2026/validasi-konsistensi.json
```

Validator memeriksa:

- checksum PDF di JSON sama dengan file PDF sumber,
- jumlah halaman JSON sama dengan `pdfinfo` dan file `page-*.txt`,
- checksum `layout.txt` dan `raw.txt`,
- tanggal ephemeris tidak duplikat dan lengkap untuk tahun yang diminta,
- tabel jam Matahari/Bulan memberi warning jika tidak berisi 25 baris,
- tabel hilal memiliki baris lokasi dan waktu,
- berkas paket sesuai manifest, ukuran file, dan SHA-256.

## Siapkan Paket Database dan Android

```bash
tools/falak/siapkan_paket_kemenag.py \
  build/falak/kemenag-2026.json \
  referensi/falaq/kemenag-2026.pdf \
  build/falak/paket-kemenag-2026
```

Paket ini berisi `manifest.json`, `metadata-database.json`,
`ephemeris-harian.json`, `hilal-lokasi.json`, `halaman-pdf.json.gz`,
`indeks-pencarian.json`, dan salinan PDF sumber.

Publikasi ke Supabase dijelaskan di `tools/falak/README_DATABASE.md`.

## Kontrak Data

JSON berisi:

- `source`: path PDF, checksum SHA-256, metadata PDF.
- `extraction`: checksum hasil ekstraksi dan catatan proses.
- `table_of_contents`: daftar isi yang dikenali.
- `sections`: heading halaman penting yang dikenali.
- `hilal_location_tables`: tabel posisi hilal per lokasi yang sudah diparse.
- `ephemeris_daily_blocks`: blok data ephemeris harian yang dikenali. Jika
  tabel harian berhasil diparse, `hourly_table.sun` dan `hourly_table.moon`
  berisi 25 baris data per jam (`00:00` sampai `24:00`) dalam bentuk raw dan
  nilai desimal.
- `pages`: teks lengkap setiap halaman agar tidak ada data PDF yang hilang.

Parser ini sengaja konservatif. Jika suatu tabel belum aman diparse menjadi
kolom-kolom numerik, teks aslinya tetap tersedia di `pages[]` dan/atau
`raw_text` pada blok terkait.
