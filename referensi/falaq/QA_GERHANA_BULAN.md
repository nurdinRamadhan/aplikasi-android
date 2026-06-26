# QA Perhitungan Gerhana Bulan Ephemeris

Dokumen ini dipakai untuk verifikasi manual di device dan audit teknis. Jika hasil aplikasi sama dengan nilai acuan di bawah dalam batas toleransi, perhitungan Gerhana Bulan Ephemeris dapat dianggap sesuai dengan contoh resmi Kemenag 2026 untuk kasus uji ini.

QA tidak cukup hanya melihat jenis gerhana. Audit harus membuka butir perhitungan karena kesalahan kecil pada FIB, Istiqbal, lintang Bulan, atau K dapat menggeser semua waktu kontak.

## Kasus Uji Utama

Sumber acuan:
- `referensi/falaq/kemenag-2026.pdf`
- Bagian: Contoh Perhitungan Gerhana Bulan pada Pertengahan Bulan Ramadan 1447 H
- Tanggal kemungkinan gerhana: 4 Maret 2026 M
- Data FIB terbesar yang dipakai: 3 Maret 2026 jam 11 GMT/UT

Input aplikasi:
- Halaman: `Falaq Ephemeris` -> tab `Hisab` -> `Buka Gerhana Bulan`
- Acuan: `Gerhana Bulan Total Maret 2026`
- Bulan Hijriah: `Pertengahan Ramadan 1447 H`
- Tanggal Kemungkinan Gerhana: `2026-03-04`
- Zona waktu: `WIB`
- Mode tanggal: `Acuan Kemenag`

## Toleransi QA

Gunakan toleransi berikut karena UI dapat membulatkan detik:

| Jenis nilai | Toleransi lulus |
|---|---:|
| Sudut ephemeris langsung | `0.05"` busur |
| Sudut hasil rumus | `0.20"` busur |
| Durasi T1/T2/T0/Delta T | `2 detik` |
| Waktu kontak lokal | `2 detik` |
| Jarak Bumi-Matahari | `0.0000002 AU` |
| FIB | `0.0001%` |
| Magnitude | `0.0001` |
| Jenis gerhana | harus sama persis |

Jika beda hanya karena tampilan UI membulatkan ke detik penuh, masih lulus. Jika beda lebih besar, cek dahulu tanggal input, zona waktu, dan apakah data FIB terbesar benar-benar tanggal `2026-03-03` jam `11 GMT/UT`.

## Nilai Acuan

| Butir | Nama | Nilai acuan |
|---:|---|---|
| 2 | FIB terbesar | `1.0000%` pada `11 GMT/UT`, 3 Maret 2026 |
| 3 | Sabaq Matahari / SM | `00° 02' 30.00"` |
| 3 | Sabaq Bulan / SB | `00° 33' 14.00"` |
| 3 | SB - SM | `00° 30' 44.00"` |
| 4 | MB | `00° 19' 21.00"` |
| 4 | Titik Istiqbal | `00h 37m 46.59s` |
| 4 | Istiqbal GMT/UT | `3 Mar 2026 11:35:57 GMT/UT` |
| 5 | Lintang Bulan jam FIB | `-00° 19' 45.00"` |
| 6 | Semi Diameter Bulan saat istiqbal | `00° 15' 36.92"` |
| 7 | Horizontal Parallax Bulan saat istiqbal | `00° 57' 18.80"` |
| 8 | Apparent Latitude Bulan saat istiqbal | `-00° 21' 35.26"` |
| 9 | Semi Diameter Matahari saat istiqbal | `00° 16' 08.03"` |
| 10 | True Geocentric Distance Matahari | `0.9913157 AU` |
| 11 | Horizontal Parallax Matahari | `00° 00' 08.87"` |
| 12 | H | `-04° 07' 54.23"` |
| 13 | U | `04° 58' 52.20"` |
| 14 | Z | `00° 21' 30.39"` |
| 15 | K | `00° 30' 50.95"` |
| 16 | D | `00° 42' 09.23"` |
| 17 | X | `00° 57' 46.15"` |
| 18 | Y | `00° 26' 32.31"` |
| 19 | Jenis gerhana | `Gerhana Bulan Total` |
| 20 | C | `00° 53' 37.02"` |
| 21 | T1 | `01h 44m 16.93s` |
| 22 | E | `00° 15' 32.93"` |
| 23 | T2 | `00h 30m 14.50s` |
| 26 | T0 | `00h 00m 24.51s` |
| 27 | Delta T | `00h 02m 16.61s` |
| 28 | Tengah Gerhana GMT/UT | `3 Mar 2026 11:33:16 GMT/UT` |
| 28 | Tengah Gerhana WIB | `3 Mar 2026 18:33:16 WIB` |
| 29 | Mulai Gerhana | `3 Mar 2026 16:48:59 WIB` |
| 30 | Mulai Total | `3 Mar 2026 18:03:02 WIB` |
| 31 | Selesai Total | `3 Mar 2026 19:03:31 WIB` |
| 32 | Selesai Gerhana | `3 Mar 2026 20:17:33 WIB` |

Catatan: PDF Kemenag pada bagian kesimpulan menulis `Tengah Gerhana : 18:33:40 WIB`, sedangkan uraian rumus pada langkah 25 menghasilkan `18:33:16.18 WIB`. Aplikasi mengikuti uraian rumus langkah 25 karena kontak gerhana pada langkah 26-29 juga dihitung dari nilai tersebut.

## Nilai yang Harus Stabil

Nilai berikut harus sama walaupun zona waktu tampilan diubah:
- FIB terbesar dan jam FIB GMT/UT;
- ELM dan ALB jam FIB;
- SM, SB, MB, titik istiqbal;
- lintang Bulan saat istiqbal;
- H, U, Z, K;
- D, X, Y, C, E;
- jenis gerhana;
- magnitude.

Nilai yang berubah jika zona waktu diganti:
- tampilan waktu lokal kontak;
- tanggal lokal jika kontak melewati tengah malam pada zona tertentu.

Gerhana Bulan tidak memakai markaz untuk menentukan kontak global pada implementasi tahap ini. Lokasi pengamat baru diperlukan pada tahap lanjutan untuk visibilitas lokal: apakah Bulan sudah terbit, sedang di atas ufuk, atau belum tampak pada kontak tertentu.

## Checklist QA Device

1. Buka halaman Gerhana Bulan dari tab Hisab.
2. Pilih acuan `Gerhana Bulan Total Maret 2026`.
3. Pastikan tanggal otomatis `2026-03-04` dan zona `WIB`.
4. Tekan `Hitung Gerhana Bulan`.
5. Pastikan kesimpulan menampilkan `Gerhana Bulan Total`.
6. Cocokkan ringkasan waktu kontak dengan tabel nilai acuan.
7. Buka accordion butir 2 sampai 32 dan cocokkan nilai utama.
8. Pastikan sumber data pada butir interpolasi menunjuk jam `11 GMT/UT` dan `12 GMT/UT` tanggal `2026-03-03`.
9. Ubah mode ke `Input Manual`, isi tanggal `2026-03-04`, zona `WIB`, lalu hitung ulang.
10. Pastikan hasil mode manual sama dengan mode acuan.
11. Ubah zona ke `WITA`; waktu lokal harus maju 1 jam dari WIB, sedangkan nilai sudut tetap.
12. Ubah zona ke `WIT`; waktu lokal harus maju 2 jam dari WIB, sedangkan nilai sudut tetap.
13. Isi tanggal manual `2026-03-03`; hasil tetap boleh menemukan FIB yang sama karena repository mengambil H-1, H, H+1, tetapi tanggal acuan QA tetap `2026-03-04`.
14. Isi tanggal yang jauh dari purnama; aplikasi harus tetap menghitung dari FIB terbesar sekitar tanggal itu, namun jenis gerhana boleh `Tidak terjadi`.

## Kriteria Lulus

QA dinyatakan lulus jika:
- Jenis gerhana sama: `Gerhana Bulan Total`.
- Semua waktu kontak utama sesuai dalam toleransi 2 detik.
- Nilai H, U, Z, K, D, X, Y, C, E sesuai dalam toleransi sudut.
- Mode acuan dan mode input manual menghasilkan nilai yang sama.
- Tidak ada error cache/data ketika memakai paket Kemenag 2026.

Jika salah satu nilai inti meleset di luar toleransi, jangan lanjut ke Gerhana Matahari sebelum penyebabnya ditemukan.

## Risiko dan Sumber Perbedaan

Perbedaan kecil dapat muncul dari:
- pembulatan detik di PDF dan UI;
- konversi durasi dari derajat ke jam;
- pembulatan `Delta T`;
- nilai tengah gerhana di kesimpulan PDF yang berbeda dari uraian rumus.

Perbedaan besar biasanya berasal dari:
- tanggal kemungkinan gerhana salah;
- aplikasi mengambil FIB terbesar dari tanggal yang salah;
- paket ephemeris tidak aktif atau cache belum sinkron;
- kolom `true_geocentric_distance_au` tidak terbaca;
- membandingkan waktu GMT/UT dengan WIB;
- menganggap tanggal purnama lokal sama dengan tanggal data GMT/UT tanpa memeriksa jam.

## Catatan Audit T0

Pada contoh Kemenag, rumus T0 ditulis:

`T0 = sin 0.05 x Ta x Tb`

Jika Ta dan Tb dipakai sebagai angka desimal murni, hasilnya menjadi sekitar 10 kali lebih besar. Aplikasi mengikuti skala hasil contoh resmi sehingga T0 mendekati `00h 00m 24.51s`. Ini wajib dicatat dalam audit karena T0 memengaruhi tengah gerhana dan seluruh waktu kontak.

## Kriteria Gagal

QA gagal jika terjadi salah satu kondisi berikut:
- jenis bukan `Gerhana Bulan Total`;
- FIB terbesar bukan jam `11 GMT/UT` tanggal `2026-03-03`;
- Istiqbal GMT/UT meleset lebih dari 2 detik;
- H, U, Z, atau K meleset lebih dari toleransi sudut;
- D, X, Y, C, atau E meleset lebih dari toleransi sudut;
- waktu mulai/selesai gerhana meleset lebih dari 2 detik;
- sumber data interpolasi bukan jam `11` dan `12 GMT/UT`;
- mode acuan dan input manual menghasilkan nilai berbeda untuk tanggal/zona yang sama.

## Unit Test Pendukung

Perintah verifikasi lokal:

```bash
./gradlew --no-daemon :app:testDebugUnitTest --tests com.alhasanah.alhasanahmedia.domain.falak.GerhanaBulanEphemerisCalculatorTest --tests com.alhasanah.alhasanahmedia.data.repository.GerhanaBulanRepositoryTest
```

Perintah cek kompilasi:

```bash
./gradlew --no-daemon :app:compileDebugKotlin
```
