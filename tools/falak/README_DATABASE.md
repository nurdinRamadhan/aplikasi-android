# Skema Database Falak Ephemeris

Migration:

```text
supabase/migrations/20260525032338_falak_ephemeris_dataset_manifest.sql
```

## Prinsip

Data besar seperti JSON ephemeris, teks halaman PDF, dan PDF sumber disimpan di
Supabase Storage bucket `falak-ephemeris`. Database hanya menyimpan metadata,
versi, daftar berkas, indeks pencarian ringan, dan kamus istilah.

Pendekatan ini membuat data Kemenag/SIMBI dapat diganti tanpa update APK:

1. Upload file JSON/PDF baru ke Storage.
2. Insert/update `falak_paket_data` dan `falak_berkas_data`.
3. Aktifkan paket baru dengan `status = 'aktif'`.
4. Android melihat manifest terbaru, mengunduh file, memvalidasi SHA-256, lalu
   mengganti cache lokal.

## Tabel

### `falak_paket_data`

Paket dataset per sumber dan tahun, misalnya `kemenag-2026`.

Kolom penting:

- `kode`: kode stabil dataset, contoh `kemenag-2026`.
- `tahun`: tahun Masehi dataset.
- `versi`: versi dataset, contoh `2026.1`.
- `jenis_sumber`: `kemenag`, `jpl`, `swiss`, `winhisab`, `pesantren`,
  `manual`, atau `lainnya`.
- `status`: `draf`, `aktif`, `arsip`, atau `ditarik`.
- `path_manifest_storage`: path file manifest di bucket `falak-ephemeris`.
- `sha256_pdf` dan `sha256_manifest`: checksum untuk audit.
- `jumlah_halaman`, `jumlah_hari_ephemeris`, `jumlah_tabel_hilal`: ringkasan
  isi PDF/JSON.

Hanya satu paket `aktif` per `jenis_sumber + tahun`.

### `falak_berkas_data`

Daftar file yang membentuk paket dataset.

Jenis berkas:

- `manifest`
- `ephemeris_harian`
- `hilal_lokasi`
- `halaman_pdf`
- `pdf_sumber`
- `indeks_pencarian`
- `istilah`
- `lainnya`

Kolom `wajib_diunduh` dipakai Android untuk menentukan file minimal yang harus
tersedia sebelum dataset dianggap siap offline.

### `falak_indeks_data`

Indeks pencarian ringan untuk halaman Ephemeris Falak di Android.

Contoh indeks:

- tanggal ephemeris: `2026-01-01`, `jam_ut = 5`.
- lokasi hilal: `Banda Aceh`, `Jakarta`, `Jayapura`.
- halaman PDF: nomor halaman dan ringkasan.
- istilah: `Equation of Time`, `Deklinasi Matahari`.

Data detail tetap dibaca dari JSON lokal melalui `path_json_pointer`.

### `falak_istilah_ephemeris`

Kamus istilah untuk membantu tiga kelompok pengguna:

- `penjelasan_awam`
- `penjelasan_santri`
- `penjelasan_praktisi`

Istilah awal yang di-seed:

- Bujur Ekliptika Matahari
- Deklinasi Matahari
- Perata Waktu
- Bujur Bulan
- Lintang Bulan
- Horizontal Paralaks Bulan
- Semi Diameter Bulan
- Fraksi Iluminasi Bulan

## RLS

Data aktif dapat dibaca publik (`anon` dan `authenticated`) agar fitur Falak
umum tidak wajib login.

Write access hanya untuk role admin:

- `super_admin`
- `rois`
- `dewan`

Role `kesantrian` diberi akses baca semua paket, termasuk draf/arsip, untuk
kebutuhan review internal.

## Storage

Bucket:

```text
falak-ephemeris
```

Bucket dibuat public-read karena dataset ephemeris bukan data pribadi dan akan
dipakai oleh halaman umum. Upload/update/delete tetap dibatasi oleh RLS
`storage.objects` untuk admin.

Struktur path yang disarankan:

```text
kemenag/2026/manifest.json
kemenag/2026/ephemeris-harian.json
kemenag/2026/hilal-lokasi.json
kemenag/2026/halaman-pdf.json.gz
kemenag/2026/kemenag-2026.pdf
```

Nama file memakai bahasa Indonesia supaya mudah dipahami developer berikutnya.

## Menyiapkan Paket Kemenag

Setelah PDF resmi diekstrak dan diparse menjadi JSON audit, pecah paket untuk
Storage dan Android:

```bash
tools/falak/siapkan_paket_kemenag.py \
  build/falak/kemenag-2026.json \
  referensi/falaq/kemenag-2026.pdf \
  build/falak/paket-kemenag-2026
```

Output utama:

- `manifest.json`: file pertama yang diunduh Android.
- `metadata-database.json`: payload untuk tabel `falak_paket_data`,
  `falak_berkas_data`, dan `falak_indeks_data`.
- `ephemeris-harian.json`: 365 hari data Matahari dan Bulan per jam UT.
- `hilal-lokasi.json`: tabel hilal lokasi Indonesia.
- `halaman-pdf.json.gz`: seluruh teks halaman PDF, tetap disimpan untuk audit.
- `indeks-pencarian.json`: indeks tanggal, jam UT, lokasi, bulan Hijriah, dan
  halaman PDF.
- `kemenag-2026.pdf`: salinan PDF resmi untuk arsip sumber.

## Publish ke Supabase

Publisher membaca `metadata-database.json`, mengunggah semua berkas ke bucket
`falak-ephemeris`, lalu mengisi tabel manifest.

Dry-run:

```bash
tools/falak/publikasikan_paket_supabase.py \
  build/falak/paket-kemenag-2026 \
  --dry-run
```

Publish sungguhan membutuhkan service role key di environment terminal admin:

```bash
export SUPABASE_URL="https://PROJECT_REF.supabase.co"
export SUPABASE_SERVICE_ROLE_KEY="..."

tools/falak/publikasikan_paket_supabase.py \
  build/falak/paket-kemenag-2026
```

Catatan keamanan:

- Jangan simpan `SUPABASE_SERVICE_ROLE_KEY` di repo.
- Publisher memakai service role hanya untuk proses admin lokal.
- Aplikasi Android tetap memakai publishable/anon key dan hanya membaca paket
  `status = 'aktif'`.
