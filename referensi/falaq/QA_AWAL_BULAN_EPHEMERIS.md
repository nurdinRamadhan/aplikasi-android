# QA Hisab Awal Bulan Ephemeris

Dokumen ini dipakai untuk audit manual dan teknis fitur `Hisab Awal Ephemeris Awal Bulan`. Tujuannya memastikan hasil aplikasi presisi terhadap contoh resmi Kemenag 2026, serta menjelaskan kemungkinan perbedaan yang perlu dipahami praktisi, pengajar, dan santri.

## Prinsip QA

Perhitungan dianggap sah untuk kasus uji ini jika:
- input tanggal, markaz, elevasi, zona waktu, data ephemeris, dan kriteria sama;
- hasil tiap butir utama sama dengan nilai acuan dalam batas toleransi;
- kesimpulan aplikasi mengikuti logika kriteria yang dipilih;
- mode acuan dan mode input manual memberi hasil sama untuk input yang sama;
- sumber ephemeris yang tampil menunjuk tanggal dan jam yang benar.

QA tidak boleh hanya mencocokkan kesimpulan akhir. Kesimpulan akhir bisa benar walau salah satu rumus tengah keliru. Audit harus membuka accordion rumus dan memeriksa nilai antara.

## Kasus Uji Utama: Awal Ramadan 1447 H

Sumber acuan:
- `referensi/falaq/kemenag-2026.pdf`
- Bagian: `Contoh Perhitungan Awal Bulan Ramadan 1447 H`
- Tanggal situasi hilal/rukyat: `17 Februari 2026 M`
- Data FIB terkecil: `17 Februari 2026`, jam `11 GMT/UT`

Input aplikasi:
- Halaman: `Falaq Ephemeris` -> tab `Hisab` -> `Buka Hisab Hilal`
- Bulan target: `Ramadan 1447 H`
- Mode tanggal: `Acuan Kemenag` atau `Input Manual`
- Tanggal Rukyat / 29 Bulan Sebelumnya: `2026-02-17`
- Kriteria: `Kemenag/MABIMS terbaru`
- Markaz:
  - Nama: `POB Cibeas, Sukabumi`
  - Lintang: `-7.0738889`
  - Bujur: `106.5311111`
  - Elevasi: `137`
  - Zona: `WIB`

Konversi koordinat:
- Lintang `-07° 04' 26"` = `-7.0738889`
- Bujur `106° 31' 52"` = `106.5311111`

Jangan memakai markaz default aplikasi untuk QA resmi Kemenag. Markaz default hanya cocok untuk uji lokasi pengguna, bukan untuk mencocokkan contoh PDF.

## Toleransi QA

Gunakan toleransi berikut:

| Jenis nilai | Toleransi lulus |
|---|---:|
| Sudut ephemeris langsung | `0.05"` busur |
| Sudut hasil rumus | `0.20"` busur |
| Waktu ijtimak/ghurub/kontak | `2 detik` |
| Waktu lama hilal/umur hilal | `2 detik` |
| FIB/Illuminasi | `0.01%` |
| Nurul Hilal | `0.01 jari` |
| Status kriteria | harus sama |
| Tanggal prakiraan | harus sama |

Jika beda hanya karena tampilan UI membulatkan ke detik penuh, masih lulus. Jika beda lebih dari toleransi, cek input markaz, zona waktu, tanggal, dan paket ephemeris sebelum menyimpulkan rumus salah.

## Nilai Acuan Kemenag

| Butir | Nama | Nilai acuan |
|---:|---|---|
| 1 | Bulan dan tahun | `Ramadan 1447 H` |
| 2 | Markaz | POB Cibeas, Sukabumi, Jawa Barat |
| 2 | Lintang | `-07° 04' 26"` |
| 2 | Bujur | `106° 31' 52"` |
| 2 | Elevasi | `137 m` |
| 3 | Tanggal 29 Sya'ban 1447 H | `17 Februari 2026 M`, Selasa Kliwon |
| 5 | FIB terkecil | `0.0001` pada jam `11 GMT/UT` |
| 6 | ELM jam 11 | `328° 47' 10"` |
| 6 | ELM jam 12 | `328° 49' 41"` |
| 6 | B1 / SM | `00° 02' 31"` |
| 7 | ALB jam 11 | `328° 16' 15"` |
| 7 | ALB jam 12 | `328° 49' 10"` |
| 7 | B2 | `00° 32' 55"` |
| 8 | MB | `00° 30' 55"` |
| 9 | SB / Sabaq Bulan Mu'addal | `00° 30' 24"` |
| 10 | Titik Ijtimak | `01h 01m 01.18s` |
| 11 | Ijtimak GMT/UT | `17 Feb 2026 12:01:01 GMT/UT` |
| 11 | Ijtimak WIB | `17 Feb 2026 19:01:01 WIB` |
| 12 | Dip awal | `00° 20' 34.61"` |
| 12 | Perkiraan ghurub awal UT | `11h 18m 40.49s` |
| 13 | Deklinasi Matahari ghurub | `-11° 53' 28.50"` |
| 13 | Semi Diameter Matahari ghurub | `00° 16' 11.09"` |
| 13 | Equation of Time ghurub | `-00h 13m 57.00s` |
| 14 | Tinggi Matahari | `-01° 11' 15.70"` |
| 15 | Sudut waktu Matahari | `92° 43' 16.23"` |
| 16 | Ghurub GMT/UT | `17 Feb 2026 11:18:42.61 GMT/UT` |
| 16 | Ghurub WIB | `17 Feb 2026 18:18:42.61 WIB` |
| 17 | AR Matahari ghurub | `330° 56' 25.53"` |
| 18 | AR Bulan ghurub | `330° 56' 25.22"` |
| 19 | Deklinasi Bulan ghurub | `-12° 55' 07.38"` |
| 20 | Semi Diameter Bulan ghurub | `00° 15' 32.05"` |
| 21 | Horizontal Parallax Bulan ghurub | `00° 57' 00.62"` |
| 22 | Sudut waktu Bulan | `92° 43' 16.54"` |
| 23 | Tinggi hilal hakiki | `-01° 03' 13.06"` |
| 24 | Parallax Bulan | `00° 57' 00.04"` |
| 25 | Ho / tinggi geosentrik terkoreksi | `-01° 44' 41.05"` |
| 26 | Refraksi hitung | `00° 44' 44.96"` |
| 26 | Refraksi dipakai karena Ho < 0° | `00° 34' 30"` |
| 27 | Tinggi hilal mar'i tepi atas | `-00° 49' 36.44"` |
| 27 | Tinggi hilal mar'i pusat | `-01° 05' 08.49"` |
| 27 | Tinggi hilal mar'i tepi bawah | `-01° 20' 40.54"` |
| 28 | Nishful Fadhlah | `01° 37' 51.90"` |
| 29 | Parallax Nishful Fadhlah | `00° 56' 59.23"` |
| 30 | Setengah Busur Siang Hakiki | `91° 37' 51.90"` |
| 31 | Setengah Busur Siang | `91° 51' 29.33"` |
| 32 | Lama Hilal / Mukuts | `-00h 03m 27.15s` |
| 33 | Terbenam Bulan | `17 Feb 2026 18:15:15.46 WIB` |
| 34 | Azimut Matahari dari Utara | `257° 51' 46.24"` |
| 35 | Azimut Bulan dari Utara | `256° 50' 38.21"` |
| 36 | Posisi Hilal | `-01° 01' 08.03"`, Selatan Matahari |
| 37 | Azimut Terbenam Bulan | `256° 57' 08.64"` |
| 38 | Illuminasi Bulan | `0.01%` |
| 39 | Nurul Hilal | `0.09 jari` |
| 40 | Kemiringan | `50° 56' 32.05"` |
| 40 | Keadaan Hilal | Telungkup, tanduk miring ke Utara |
| 41 | Elongasi Toposentrik | `01° 02' 49.18"` |
| 41 | Elongasi Geosentrik | `01° 01' 38.88"` |
| 42 | Umur Hilal | `-00h 42m 18.57s` |

## Nilai yang Harus Cocok di Aplikasi

Aplikasi saat ini menampilkan 32 butir inti. Pemetaan dengan contoh Kemenag:

| Aplikasi | Dokumen Kemenag | Catatan audit |
|---:|---:|---|
| 1 | 2 | Markaz harus sama persis. |
| 2-4 | 5-11 | Ijtimak, FIB, ALB, ELM, SB, SM. |
| 5-8 | 12-16 | Posisi Matahari dan ghurub. |
| 9-12 | 17-22 | Interpolasi AR/deklinasi dan sudut waktu Bulan. |
| 13-18 | 23-27 | Tinggi hilal hakiki, parallax, Ho, refraksi, tinggi mar'i. |
| 19-24 | 28-33 | NF, PNF, SBSH, SBS, mukuts, terbenam hilal. |
| 25-28 | 34-37 | Azimut dan posisi hilal. |
| 29-32 | 38-41 | FIB ghurub, nurul hilal, kemiringan, elongasi. |

Catatan penting: fitur aplikasi memakai elongasi geosentrik untuk evaluasi MABIMS. Dokumen Kemenag menampilkan toposentrik dan geosentrik; untuk syarat MABIMS, cocokkan nilai geosentrik aplikasi dengan `JBg = 01° 01' 38.88"` pada contoh Kemenag.

## Kesimpulan yang Diharapkan

Dengan kriteria `Kemenag/MABIMS terbaru`:
- Ijtimak sebelum/saat ghurub: `belum memenuhi`, karena ijtimak `19:01:01 WIB` terjadi setelah ghurub `18:18:42 WIB`.
- Tinggi hilal mar'i: `belum memenuhi`, karena tinggi tepi atas `-00° 49' 36.44"` dan pusat `-01° 05' 08.49"` masih di bawah ufuk.
- Elongasi geosentrik: `belum memenuhi`, karena sekitar `01° 01' 38.88"` < `06° 24' 00"`.
- Tanggal prakiraan awal Ramadan: `19 Februari 2026` jika memakai logika istikmal dari tanggal rukyat 17 Februari 2026.

Catatan koreksi bacaan PDF: bagian akhir contoh Kemenag memuat kalimat bahwa ijtimak terjadi sebelum Matahari terbenam, tetapi angka yang ditampilkan menunjukkan ijtimak terjadi setelah ghurub. Untuk QA, ikuti angka dan aritmetika: `19:01:01 WIB` setelah `18:18:42 WIB`.

## Checklist QA Device

1. Pastikan paket `kemenag-2026` aktif dan data ephemeris 2026 sudah tersinkron.
2. Buka `Falaq Ephemeris` dan cek tab `Paket`; status harus aktif.
3. Buka tab `Harian`, pilih `17 Februari 2026`, pastikan tabel Matahari/Bulan jam `11` dan `12 GMT/UT` tersedia.
4. Buka tab `Hisab`, tekan `Buka Hisab Hilal`.
5. Pilih `Ramadan 1447 H`.
6. Pakai mode `Acuan Kemenag`, atau mode `Input Manual` dengan tanggal `2026-02-17`.
7. Isi markaz POB Cibeas:
   - lintang `-7.0738889`
   - bujur `106.5311111`
   - elevasi `137`
   - zona `WIB`
8. Pilih kriteria `Kemenag/MABIMS terbaru`.
9. Tekan `Hitung`.
10. Cocokkan ringkasan: ijtimak, ghurub, tinggi hilal, mukuts, terbenam hilal, azimut, FIB, nurul hilal, elongasi.
11. Buka accordion butir 1-32 dan cocokkan nilai perantara sesuai tabel acuan.
12. Pastikan sumber data interpolasi memakai tanggal `2026-02-17`, jam `11` dan `12 GMT/UT` untuk data ghurub.
13. Ubah mode ke `Input Manual` tanpa mengubah tanggal/markaz/kriteria, hitung ulang.
14. Pastikan hasil mode manual sama dengan mode acuan.
15. Ubah kriteria ke `Wujudul Hilal`, lalu pastikan hasil tetap tidak memenuhi karena tinggi hilal di bawah 0°.
16. Ubah kriteria ke `Tanpa kriteria`, lalu pastikan aplikasi tidak memakai batas visibilitas sebagai syarat, tetapi nilai astronomis tetap sama.

## QA Variasi Markaz

Fitur ini mendukung markaz manual, GPS, dan map. Untuk audit:

| Skenario | Ekspektasi |
|---|---|
| Markaz POB Cibeas manual | Harus cocok dengan contoh Kemenag. |
| Markaz default aplikasi | Nilai tinggi, azimut, ghurub, mukuts boleh berbeda. Ijtimak tetap sama karena geosentrik. |
| Deteksi GPS | Nilai lokal berubah sesuai lintang/bujur/elevasi pengguna. Cocokkan dengan hitung ulang manual memakai koordinat GPS yang sama. |
| Elevasi 0 m vs 137 m | Dip berubah; tinggi mar'i dan ghurub ikut berubah. |
| Zona salah | Waktu lokal bergeser, tetapi GMT/UT harus tetap konsisten. |

Nilai yang tidak boleh berubah saat markaz diganti:
- waktu ijtimak GMT/UT;
- FIB terkecil;
- ALB/ELM pada jam FIB;
- SB/SM ijtimak.

Nilai yang boleh berubah saat markaz diganti:
- ghurub;
- tinggi hilal;
- azimut;
- lama hilal;
- terbenam hilal;
- posisi hilal;
- sebagian nilai interpolasi saat ghurub karena jam ghurub berubah.

## QA Variasi Kriteria

| Kriteria | Syarat | Catatan |
|---|---|---|
| Kemenag/MABIMS terbaru | ijtimak sebelum/saat ghurub, tinggi hilal >= 3°, elongasi >= 6.4° | Default aplikasi. |
| Wujudul Hilal | ijtimak sebelum/saat ghurub dan tinggi hilal >= 0° | Tidak memakai syarat elongasi minimum. |
| Tanpa kriteria | tidak memakai syarat visibilitas | Dipakai untuk belajar nilai astronomis, bukan penetapan resmi. |

Pada contoh Ramadan 1447 H, `Kemenag/MABIMS terbaru` dan `Wujudul Hilal` sama-sama tidak memenuhi.

## Risiko dan Sumber Perbedaan

Perbedaan kecil dapat muncul dari:
- pembulatan detik pada PDF dan UI;
- input koordinat DMS yang salah dikonversi ke desimal;
- elevasi GPS yang berbeda dari elevasi resmi markaz;
- penggunaan zona waktu yang salah;
- data ephemeris belum tersinkron atau paket tahun tidak aktif;
- membandingkan elongasi toposentrik dengan geosentrik;
- memakai tanggal awal bulan, bukan tanggal rukyat/29 bulan sebelumnya;
- memakai markaz Kemenag tabel hilal lain, bukan POB Cibeas.

Perbedaan besar biasanya berasal dari:
- tanggal salah satu hari;
- bujur timur diberi tanda negatif;
- lintang selatan diberi tanda positif;
- memakai markaz default;
- data ephemeris bukan paket resmi Kemenag 2026;
- mode kriteria berbeda.

## Kriteria Lulus

QA lulus jika:
- data sumber dan markaz sesuai;
- ijtimak WIB cocok dalam 2 detik;
- ghurub WIB cocok dalam 2 detik;
- tinggi hilal mar'i cocok dalam 0.20 detik busur;
- azimut Matahari dan Bulan cocok dalam 0.20 detik busur;
- mukuts dan terbenam hilal cocok dalam 2 detik;
- FIB ghurub cocok sampai `0.01%`;
- elongasi geosentrik cocok dalam 0.20 detik busur;
- kesimpulan `belum memenuhi Kemenag/MABIMS terbaru`;
- tanggal prakiraan `19 Februari 2026`;
- mode acuan dan manual sama.

Jika salah satu nilai inti gagal, hentikan audit dan catat:
- screenshot input;
- screenshot ringkasan;
- nomor butir yang berbeda;
- nilai aplikasi;
- nilai acuan;
- paket data aktif;
- apakah sumber data butir menunjuk jam dan tanggal yang benar.

## Unit Test Pendukung

Perintah verifikasi lokal:

```bash
./gradlew --no-daemon :app:testDebugUnitTest --tests com.alhasanah.alhasanahmedia.domain.falak.HisabHilalEphemerisCalculatorTest --tests com.alhasanah.alhasanahmedia.data.repository.HisabHilalRepositoryTest
```

Perintah cek kompilasi:

```bash
./gradlew --no-daemon :app:compileDebugKotlin
```
