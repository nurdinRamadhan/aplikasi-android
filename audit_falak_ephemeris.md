# Audit Perhitungan Falak Ephemeris

Audit ini memetakan dokumen prosedur `referensi/falaq/EPHEMERIS AWAL BULAN.docx` ke implementasi Kotlin awal bulan/hisab hilal pada project Android.

## Status Kelengkapan Audit

Dokumen ini disusun untuk dua kebutuhan:

- **Audit kepatuhan:** memastikan setiap tahap dalam dokumen prosedur awal bulan memiliki padanan implementasi Kotlin, serta mencatat perbedaan formula, normalisasi, dan asumsi teknis.
- **Edukasi teknis:** menjelaskan alur algoritma hisab hilal ephemeris secara bertahap agar pembaca dapat memahami mengapa kode tertentu dipakai, data apa yang dibutuhkan, dan bagian mana yang perlu diuji ulang saat sumber data atau kriteria berubah.

Status saat ini:

- Tahap utama dokumen: **32 tahap**.
- Tahap yang ditemukan implementasinya: **32 tahap**.
- Tahap utama yang belum ditemukan implementasinya: **0 tahap**.
- Rumus tambahan setelah kesimpulan dokumen: ada blok visualisasi fisik berbasis jarak 500 cm. Blok ini **belum menjadi bagian kalkulator utama** dan dibahas pada bagian gap.

Catatan penting: audit ini menilai kesesuaian struktur rumus dan alur implementasi terhadap dokumen prosedur. Audit ini belum membuktikan akurasi numerik terhadap dataset pembanding eksternal untuk banyak tanggal/markaz. Untuk audit numerik penuh, lihat bagian **Rekomendasi Pengujian Audit**.

## Ringkasan Algoritma

Algoritma awal bulan ephemeris di kode berjalan dalam urutan berikut:

1. Menentukan konteks perhitungan: bulan Hijriah target, tanggal situasi hilal/rukyat, markaz, zona waktu, dan kriteria awal bulan.
2. Menyiapkan data ephemeris harian untuk tanggal situasi hilal dan tanggal setelahnya.
3. Mencari FIB Bulan terkecil sebagai pendekatan waktu ijtimak.
4. Mengambil longitude Bulan/Matahari di sekitar jam FIB, lalu menghitung sabaq Bulan dan sabaq Matahari.
5. Menghitung waktu ijtimak lokal dari jarak longitude dan sabaq relatif.
6. Menghitung ghurub: tinggi Matahari haqiqi, sudut waktu Matahari, KWD, dan equation of time.
7. Menginterpolasi data Matahari/Bulan pada waktu ghurub.
8. Menghitung tinggi hilal haqiqi, parallax, semi diameter, refraksi, dip, dan tinggi hilal mar'i.
9. Menghitung elemen visibilitas: lama hilal, terbenam hilal, azimut, posisi relatif, arah terbenam, fraction illumination, nurul hilal, kemiringan, dan elongasi.
10. Menentukan kesimpulan awal bulan berdasarkan kriteria: ijtimak sebelum/saat ghurub, tinggi minimum, dan elongasi minimum.

Karakter implementasi:

- Data ephemeris diperlakukan sebagai tabel per jam dalam GMT/UT.
- Waktu lokal dikonversi ke GMT/UT dengan `offsetJam`.
- Interpolasi umum memakai formula dokumen `Na - (Na - Nb) * Nc`.
- Untuk sudut yang dapat melewati 0/360 derajat, kode memakai normalisasi sudut agar hasil tetap kontinu.
- Beberapa argumen trigonometri dikunci dengan `coerceIn(-1.0, 1.0)` sebelum `acos/asin` untuk mencegah error numerik akibat floating point.

## Data, Satuan, dan Konvensi

Konvensi penting yang dipakai kode:

- Sudut disimpan dalam **derajat desimal**.
- Waktu disimpan dalam **jam desimal**.
- Data ephemeris jam per jam memakai **GMT/UT**.
- Zona waktu Indonesia:
  - WIB: offset 7 jam, bujur standar 105 derajat.
  - WITA: offset 8 jam, bujur standar 120 derajat.
  - WIT: offset 9 jam, bujur standar 135 derajat.
- Refraksi rata-rata yang dipakai: `34.5 / 60 = 0.575 derajat`, setara `0 derajat 34 menit 30 detik`.
- Dip dihitung dengan `sqrt(elevasi) * 0.0293`.
- KWD dalam jam: `(bujur standar - bujur markaz) / 15`.
- Nilai lintang/bujur mengikuti tanda desimal umum:
  - Lintang selatan negatif.
  - Bujur timur positif.

Kemungkinan sumber perbedaan hasil:

- Pembulatan derajat-menit-detik pada dokumen manual versus derajat desimal di kode.
- Data ephemeris mentah yang berbeda tahun, versi, atau sumber.
- Penggunaan `ModeInterpolasi.SUDUT_MAJU` untuk longitude/right ascension ketika melewati 0/360 derajat.
- Perbedaan penentuan tanggal situasi hilal, terutama bila ijtimak/ghurub melewati tengah malam lokal.
- Elevasi otomatis dari lokasi dapat berbeda dari elevasi markaz resmi.
- Refraksi aktual atmosfer dapat berbeda dari refraksi rata-rata atau rumus sederhana.

## Metodologi Audit

Audit dilakukan dengan cara berikut:

1. **Ekstraksi prosedur dokumen.**
   Dokumen `EPHEMERIS AWAL BULAN.docx` dibaca sebagai prosedur 32 tahap. Setiap tahap diidentifikasi dari nomor, nama istilah, deskripsi perhitungan, dan rumus yang tertulis.

2. **Inventaris implementasi Kotlin.**
   File Kotlin dicari pada paket `domain/falak`, `data/model/falak`, `data/repository`, `ui/falak`, `util`, dan `di`. File di luar alur hisab hilal awal bulan dipisahkan sebagai tidak relevan.

3. **Pemetaan tahap ke kode.**
   Setiap tahap dokumen dipetakan ke fungsi numerik yang menghitung nilai tersebut, bukan hanya ke tampilan UI. Jika ada fungsi `tampilkan...`, fungsi itu dipakai sebagai bukti bahwa aplikasi juga menyimpan rumus/substitusi/hasil untuk audit.

4. **Pemeriksaan dependency.**
   Model konteks, markaz, ephemeris, helper interpolasi, helper normalisasi sudut, helper trigonometri derajat, dan repository data dimasukkan sebagai prasarana karena dipakai lintas tahap.

5. **Pencatatan perbedaan.**
   Jika kode melakukan normalisasi, pembulatan, clamp numerik, atau formula yang berbeda dari teks dokumen, perbedaan dicatat sebagai penguatan atau potensi penyesuaian.

6. **Penilaian risiko.**
   Setiap area yang berpotensi mengubah hasil hisab dicatat dalam matriks risiko: data ephemeris, zona waktu, interpolasi, refraksi, pembulatan, kriteria awal bulan, dan validasi data.

Yang belum termasuk dalam audit ini:

- Belum ada pembuktian numerik dengan banyak tanggal dan markaz.
- Belum ada perbandingan langsung terhadap output software astronomi lain.
- Belum ada validasi fisik terhadap hasil rukyat aktual.
- Belum ada validasi keputusan resmi pemerintah, karena kode menghasilkan prakiraan hisab, bukan keputusan isbat.

## Glosarium Teknis

| Istilah | Makna dalam dokumen/kode | Catatan audit |
|---|---|---|
| Markaz | Titik lokasi perhitungan | Berisi nama, lintang, bujur, elevasi, zona waktu |
| Phi / `phi` | Lintang tempat | Di kode: `lintangDerajat` |
| Lambda | Bujur tempat | Di kode: `bujurDerajat` |
| Elevasi | Tinggi tempat dari permukaan laut | Dipakai untuk `dip` |
| GMT/UT | Waktu tabel ephemeris | Kode memakai label `"GMT/UT"` |
| WIB/WITA/WIT | Waktu lokal Indonesia | Offset 7/8/9 jam dari UT |
| FIB | Fraction Illumination Bulan | Di kode: `fraction_illumination_percent` |
| ALB | Apparent Longitude Bulan | Di kode: `apparent_longitude` tabel Bulan |
| ELM | Ecliptic Longitude Matahari | Di kode: `apparent_ecliptic_longitude` tabel Matahari |
| SB | Sabaq Bulan | Selisih ALB satu jam |
| SM | Sabaq Matahari | Selisih ELM satu jam |
| Ijtimak | Konjungsi Bulan-Matahari | Didekati dari ALB/ELM dan sabaq |
| Sd Matahari | Semi diameter Matahari | Di kode: `semi_diameter` tabel Matahari |
| Sd Bulan | Semi diameter Bulan | Di kode: `semi_diameter` tabel Bulan |
| Dip | Kerendahan ufuk | `sqrt(elevasi) * 0.0293` |
| Equation of Time | Perata waktu Matahari | Di kode: `equation_of_time` dalam jam |
| AR | Apparent Right Ascension | Di kode: `apparent_right_ascension` |
| Deklinasi | Apparent Declination | Di kode: `apparent_declination` |
| HP | Horizontal Parallax | Di kode: `horizontal_parallax` |
| ho | Tinggi hilal sebelum refraksi/dip akhir | `h bulan - parallax + Sd bulan` |
| h' | Tinggi hilal mar'i | `ho + refraksi + dip` |
| NF | Nishful Fadhlah | Komponen setengah busur siang |
| PNF | Parallax Nishful Fadhlah | `cos(NF) * HP` |
| SBSH | Setengah Busur Siang Haqiqi | `90 + NF` |
| SBS | Setengah Busur Siang | SBSH dengan koreksi PNF, SD, refraksi, dip |
| LM / Mukuts | Lama hilal | `(SBS - t bulan) / 15` |
| TRB | Terbenam Hilal | `ghurub + LM` |
| Az | Azimut dari titik Barat | Bukan azimut kompas 0-360 |
| PH | Posisi Hilal | `Az Bulan - Az Matahari` |
| AT | Arah Terbenam Hilal | Rumus azimut berbasis SBS |
| NH | Nurul Hilal | Lebar cahaya hilal satuan jari |
| MH | Kemiringan Hilal | `atan(PH / h')` |
| JB | Jarak Busur / Elongasi | Jarak sudut Matahari-Bulan |

## Peta Dependency Fungsi

Peta ini menjelaskan hubungan fungsi agar auditor memahami mengapa satu perubahan kecil dapat mempengaruhi banyak tahap.

| Fungsi/Class | Peran | Dipakai oleh tahap |
|---|---|---|
| `KonteksHisabHilal` | Parameter utama hisab | Semua tahap |
| `MarkazFalak` | Lintang, bujur, elevasi, zona waktu | 1, 4, 5, 6, 7, 8, 13, 19, 22, 25, 26, 28 |
| `FalakEphemerisHarian` | Data ephemeris harian | 2, 5, 6, 8, 9, 10, 12, 14, 15, 29 |
| `tentukanDataIjtima` | Ambil FIB, ALB, ELM | 2 |
| `tentukanSabaqIjtima` | Hitung SB dan SM | 3 |
| `hitungSaatIjtima` | Hitung ijtimak UT/lokal | 4 |
| `hitungSaatGhurub` | Hitung waktu ghurub lokal | 8 dan menjadi input 9-12, 14-15, 29 |
| `interpolasiGhurub` | Interpolasi nilai derajat saat ghurub | 9, 10, 12, 14, 15 |
| `interpolasiAngkaGhurub` | Interpolasi nilai angka saat ghurub | 29 |
| `deltaMajuDerajat` | Selisih sudut maju | 3, 9, 10 |
| `selisihSudutBertanda` | Selisih sudut terdekat bertanda | 4, 11 |
| `normalisasiWaktu` | Normalisasi jam melewati 24/negatif | 4, 8, 9-12, 14-15, 24, 29 |
| `sinDeg`, `cosDeg`, `tanDeg` | Trigonometri input derajat | 6, 13, 14, 17, 19, 20, 25, 26, 28, 32 |
| `susunKesimpulanHisabHilal` | Evaluasi kriteria awal bulan | Kesimpulan |

## Kolom Data Ephemeris yang Dipakai

| Kolom JSON | Tabel | Tahap | Satuan/format yang diharapkan | Risiko |
|---|---|---:|---|---|
| `hour_ut` | Matahari/Bulan | Banyak tahap | Integer jam UT | Jika hilang, baris tidak bisa dipilih |
| `fraction_illumination_percent` | Bulan | 2, 29 | Persen numerik | Bisa tertukar dengan fraksi 0-1 |
| `apparent_longitude` | Bulan | 2, 3 | Objek derajat dengan `decimal_degree` | Perlu normalisasi 0-360 |
| `apparent_ecliptic_longitude` | Matahari | 2, 3 | Objek derajat dengan `decimal_degree` | Perlu normalisasi 0-360 |
| `semi_diameter` | Matahari | 5 | Objek derajat dengan `decimal_degree` | Pengaruh ke ghurub |
| `apparent_declination` | Matahari | 6, 12, 25, 32 | Objek derajat dengan `decimal_degree` | Pengaruh ke tinggi/azimut/elongasi |
| `equation_of_time` | Matahari | 8 | Objek jam dengan `hours` | Salah satuan akan menggeser ghurub |
| `apparent_right_ascension` | Matahari | 9, 11, 32 | Objek derajat dengan `decimal_degree` | Sumber tertentu mungkin memakai jam |
| `apparent_right_ascension` | Bulan | 10, 11, 32 | Objek derajat dengan `decimal_degree` | Perlu interpolasi sudut maju |
| `apparent_declination` | Bulan | 12, 13, 19, 25, 26, 28, 32 | Objek derajat dengan `decimal_degree` | Pengaruh luas |
| `horizontal_parallax` | Bulan | 14, 20 | Objek derajat dengan `decimal_degree` | Sensitif untuk hilal dekat ufuk |
| `semi_diameter` | Bulan | 15, 16, 22 | Objek derajat dengan `decimal_degree` | Pengaruh tinggi tepi atas |

## Traceability Matrix Tahap ke Implementasi

| Tahap | Nama tahap | Fungsi hitung utama | Fungsi tampilan/audit | Input utama | Output utama |
|---:|---|---|---|---|---|
| 1 | Markaz | `buildKonteks`, `MarkazFalak` | `tampilkanMarkaz` | Input lokasi | `MarkazFalak` |
| 2 | FIB, ALB, ELM | `tentukanDataIjtima` | `tampilkanDataIjtima` | Ephemeris Bulan/Matahari | `DataIjtima` |
| 3 | Sabaq | `tentukanSabaqIjtima` | `tampilkanSabaqIjtima` | `DataIjtima` | `SabaqIjtima` |
| 4 | Ijtimak | `hitungSaatIjtima` | `tampilkanSaatIjtima` | `DataIjtima`, `SabaqIjtima` | `SaatIjtima` |
| 5 | h Matahari | `hitungPosisiMatahariHaqiqiGhurub` | `tampilkanPosisiMatahariHaqiqiGhurub` | Sd Matahari, elevasi | `PosisiMatahariHaqiqiGhurub` |
| 6 | t Matahari | `hitungSudutWaktuMatahariGhurub` | `tampilkanSudutWaktuMatahariGhurub` | Lintang, deklinasi, h Matahari | `SudutWaktuMatahariGhurub` |
| 7 | KWD | `hitungKoreksiWaktuDaerah` | `tampilkanKoreksiWaktuDaerah` | Bujur standar, bujur markaz | `KoreksiWaktuDaerah` |
| 8 | Ghurub | `hitungSaatGhurub` | `tampilkanSaatGhurub` | t Matahari, EoT, KWD | `SaatGhurub` |
| 9 | AR Matahari | `hitungAsensiorektaMatahariGhurub` | `tampilkanAsensiorektaMatahariGhurub` | AR Matahari per jam | `AsensiorektaMatahariGhurub` |
| 10 | AR Bulan | `hitungAsensiorektaBulanGhurub` | `tampilkanAsensiorektaBulanGhurub` | AR Bulan per jam | `AsensiorektaBulanGhurub` |
| 11 | t Bulan | `hitungSudutWaktuBulanGhurub` | `tampilkanSudutWaktuBulanGhurub` | AR Matahari, AR Bulan, t Matahari | `SudutWaktuBulanGhurub` |
| 12 | Deklinasi | `hitungDeklinasiGhurub` | `tampilkanDeklinasiGhurub` | Deklinasi Matahari/Bulan per jam | `DeklinasiGhurub` |
| 13 | h Bulan haqiqi | `hitungTinggiBulanHaqiqiGhurub` | `tampilkanTinggiBulanHaqiqiGhurub` | Lintang, d Bulan, t Bulan | `TinggiBulanHaqiqiGhurub` |
| 14 | Parallax | `hitungParallaxBulanGhurub` | `tampilkanParallaxBulanGhurub` | HP, h Bulan | `ParallaxBulanGhurub` |
| 15 | Sd Bulan | `hitungSemiDiameterBulanGhurub` | `tampilkanSemiDiameterBulanGhurub` | Sd Bulan per jam | `SemiDiameterBulanGhurub` |
| 16 | ho | `hitungHoBulanGhurub` | `tampilkanHoBulanGhurub` | h Bulan, parallax, Sd Bulan | `HoBulanGhurub` |
| 17 | Refraksi | `hitungRefraksiHilal` | `tampilkanRefraksiHilal` | ho | `RefraksiHilal` |
| 18 | h' Bulan | `hitungTinggiBulanMariGhurub` | `tampilkanTinggiBulanMariGhurub` | ho, refraksi, dip | `TinggiBulanMariGhurub` |
| 19 | NF | `hitungNishfulFadhlahBulan` | `tampilkanNishfulFadhlahBulan` | Lintang, d Bulan | `NishfulFadhlahBulan` |
| 20 | PNF | `hitungParallaxNishfulFadhlah` | `tampilkanParallaxNishfulFadhlah` | NF, HP | `ParallaxNishfulFadhlah` |
| 21 | SBSH | `hitungSetengahBusurSiangBulanHaqiqi` | `tampilkanSetengahBusurSiangBulanHaqiqi` | NF | `SetengahBusurSiangBulanHaqiqi` |
| 22 | SBS | `hitungSetengahBusurSiangBulan` | `tampilkanSetengahBusurSiangBulan` | SBSH, NF, PNF, SD, dip | `SetengahBusurSiangBulan` |
| 23 | Mukuts | `hitungLamaHilalMukuts` | `tampilkanLamaHilalMukuts` | SBS, t Bulan | `LamaHilalMukuts` |
| 24 | Terbenam Hilal | `hitungTerbenamHilal` | `tampilkanTerbenamHilal` | Ghurub, mukuts | `TerbenamHilal` |
| 25 | Az Matahari | `hitungAzimutMatahariGhurub` | `tampilkanAzimutMatahariGhurub` | Lintang, t Matahari, d Matahari | `AzimutMatahariGhurub` |
| 26 | Az Bulan | `hitungAzimutBulanGhurub` | `tampilkanAzimutBulanGhurub` | Lintang, t Bulan, d Bulan | `AzimutBulanGhurub` |
| 27 | Posisi Hilal | `hitungPosisiHilal` | `tampilkanPosisiHilal` | Az Bulan, Az Matahari | `PosisiHilal` |
| 28 | AT | `hitungArahTerbenamHilal` | `tampilkanArahTerbenamHilal` | Lintang, SBS, d Bulan | `ArahTerbenamHilal` |
| 29 | FIB ghurub | `hitungLuasCahayaHilal` | `tampilkanLuasCahayaHilal` | FIB per jam | `LuasCahayaHilal` |
| 30 | Nurul Hilal | `hitungLebarNurulHilal` | `tampilkanLebarNurulHilal` | PH, h' | `LebarNurulHilal` |
| 31 | Kemiringan | `hitungKemiringanHilal` | `tampilkanKemiringanHilal` | PH, h' | `KemiringanHilal` |
| 32 | Elongasi | `hitungJarakBusurElongasi` | `tampilkanJarakBusurElongasi` | AR dan deklinasi Matahari/Bulan | `JarakBusurElongasi` |

## Aturan Pembulatan dan Toleransi yang Disarankan

Kode menghitung menggunakan `Double` dan baru memformat pada output. Dokumen manual biasanya membulatkan angka pada setiap tahap. Dua pendekatan ini dapat menghasilkan beda kecil.

Rekomendasi standar audit:

| Jenis nilai | Format tampilan | Toleransi audit awal | Catatan |
|---|---|---:|---|
| Waktu ijtimak/ghurub/terbenam | HH:MM:SS | 1-5 detik | Lebih ketat jika data pembanding full precision |
| Sudut utama | derajat-menit-detik | 1-10 detik busur | Tergantung pembulatan dokumen |
| FIB | persen desimal | 0,001-0,01 persen | Tergantung presisi data |
| Lama hilal | jam atau menit-detik | 1-5 detik | Perhatikan nilai negatif |
| Kriteria tinggi | derajat desimal | 0,001 derajat | Kritis jika dekat ambang 3 derajat |
| Kriteria elongasi | derajat desimal | 0,001 derajat | Kritis jika dekat ambang 6,4 derajat |

Untuk audit formal, pilih salah satu metode:

- **Full precision sampai akhir:** sesuai kode saat ini, lebih stabil secara numerik.
- **Pembulatan per tahap:** lebih menyerupai hitung manual, tetapi dapat mengakumulasi selisih.
- **Pembulatan hanya saat tampil:** kompromi yang umum untuk aplikasi.

## Edge Case yang Harus Diuji

| Kasus | Tahap terdampak | Risiko |
|---|---|---|
| Longitude Bulan dari 359 ke 0 derajat | 3 | Sabaq bisa negatif jika tidak dinormalisasi |
| Right ascension melewati 0 derajat | 9, 10, 11, 32 | Sudut waktu/elongasi bisa melompat |
| Ghurub lokal setelah tengah malam atau sebelum 0 | 8, 9-12, 24, 29 | Tanggal ephemeris harus dinormalisasi |
| Jam FIB terkecil berada di jam 23/24 | 2, 3, 4 | Butuh data jam/tanggal setelahnya |
| Elevasi negatif atau sangat tinggi | 5, 18, 22 | Dip tidak wajar |
| `ho <= 0` | 17 | Refraksi memakai nilai rata-rata |
| `h'` mendekati nol | 30, 31 | Nurul/kemiringan bisa sangat sensitif |
| SBSH tepat 90 derajat | 22 | Cabang rumus perlu keputusan eksplisit |
| Tinggi tepat di ambang 3 derajat | Kesimpulan | Pembulatan dapat mengubah status memenuhi |
| Elongasi tepat di ambang 6,4 derajat | Kesimpulan | Pembulatan dapat mengubah status memenuhi |

## Cara Membaca `ButirPerhitunganFalak`

`ButirPerhitunganFalak` adalah struktur penting untuk audit karena menyimpan jejak perhitungan per tahap.

```kotlin
data class ButirPerhitunganFalak(
    val nomor: Int,
    val judul: String,
    val rumus: String,
    val substitusi: String,
    val hasil: String,
    val catatan: String? = null,
    val sumber: List<SumberEphemerisFalak> = emptyList(),
)
```

Makna field:

- `nomor`: nomor tahap sesuai dokumen.
- `judul`: nama tahap yang ditampilkan.
- `rumus`: formula generik tahap.
- `substitusi`: formula setelah nilai aktual dimasukkan.
- `hasil`: hasil akhir tahap dalam format tampilan.
- `catatan`: informasi audit tambahan, misalnya mode normalisasi atau argumen trigonometri.
- `sumber`: daftar sumber data ephemeris, termasuk tanggal, jam UT, tabel, kolom, dan nilai raw.

Rekomendasi edukasi:

- Saat mengajar, tampilkan `rumus`, lalu `substitusi`, lalu `hasil`.
- Saat audit, periksa `sumber` untuk memastikan nilai berasal dari kolom dan jam ephemeris yang benar.
- Saat debugging, periksa `catatan`, terutama argumen trigonometri dan nilai `Nc` interpolasi.

## Detail Normalisasi Sudut dan Waktu

Normalisasi adalah bagian kecil tetapi sangat penting. Banyak kesalahan hisab digital terjadi bukan karena rumus utama salah, melainkan karena sudut atau waktu melewati batas siklus.

### Normalisasi waktu

Kode:

```kotlin
private fun normalisasiWaktu(tanggalAwal: LocalDate, jam: Double, zona: String): WaktuFalak {
    val days = floor(jam / 24.0).toLong()
    var normalizedHour = jam - (days * 24.0)
    var date = tanggalAwal.plusDays(days)
    if (normalizedHour < 0.0) {
        normalizedHour += 24.0
        date = date.minusDays(1)
    }
    return WaktuFalak(date, normalizedHour, zona)
}
```

Alasan:

- Jika ijtimak lokal lebih dari 24 jam, tanggal harus maju.
- Jika waktu UT hasil konversi negatif, tanggal harus mundur.
- Jika lama hilal negatif, terbenam hilal bisa terjadi sebelum ghurub.

Risiko jika tidak ada normalisasi:

- Ghurub jam 25:10 bisa salah ditampilkan sebagai jam 25:10 pada tanggal yang sama.
- Jam UT -1 bisa salah mengambil data ephemeris tanggal yang sama, padahal harus tanggal sebelumnya.
- Terbenam hilal negatif bisa salah dimaknai sebagai setelah ghurub.

### Normalisasi sudut maju

Kode:

```kotlin
private fun deltaMajuDerajat(awal: Double, setelah: Double): Double {
    var delta = setelah - awal
    while (delta < 0.0) delta += 360.0
    return delta
}
```

Alasan:

- Longitude/AR dapat bergerak dari `359.9` ke `0.1`.
- Selisih matematis biasa menghasilkan `0.1 - 359.9 = -359.8`, padahal gerak majunya `0.2`.

Tahap terdampak:

- Tahap 3: sabaq ALB/ELM.
- Tahap 9-10: interpolasi AR dengan mode sudut maju.

### Selisih sudut bertanda

Kode:

```kotlin
private fun selisihSudutBertanda(nilaiKiri: Double, nilaiKanan: Double): Double {
    var delta = nilaiKiri - nilaiKanan
    while (delta > 180.0) delta -= 360.0
    while (delta <= -180.0) delta += 360.0
    return delta
}
```

Alasan:

- Untuk `ELM - ALB` dan `AR Matahari - AR Bulan`, yang dibutuhkan adalah selisih terdekat bertanda, bukan selisih mentah.
- Ini mencegah hasil yang melompat ratusan derajat saat nilai melewati batas 0/360.

Catatan audit:

- Normalisasi ini adalah penyempurnaan numerik, bukan perubahan substansi rumus.
- Namun auditor perlu mencatatnya karena hasil dapat berbeda dari hitung manual yang tidak mengantisipasi 0/360.

## Validasi Input yang Perlu Diperketat

Kode saat ini sudah memvalidasi sebagian input:

- `LocalDate.parse(...)` memastikan tanggal format ISO valid.
- `toDoubleOrNull()` memastikan lintang, bujur, elevasi bisa dibaca sebagai angka.
- `FalakMarkazProvider.validasiKoordinat(...)` memastikan lintang/bujur dalam rentang normal pada jalur provider.

Validasi tambahan yang disarankan untuk audit formal:

| Input | Validasi yang disarankan | Alasan |
|---|---|---|
| Bulan Hijriah | Tidak kosong dan sesuai daftar bulan | Menghindari label kesimpulan salah |
| Tanggal situasi hilal | Harus tanggal 29 bulan sebelumnya menurut acuan | Menghindari salah tanggal rukyat |
| Lintang | `-90..90` pada semua jalur input | ViewModel manual belum eksplisit memeriksa rentang |
| Bujur | `-180..180` pada semua jalur input | ViewModel manual belum eksplisit memeriksa rentang |
| Elevasi | Batas wajar, misalnya `-500..9000` meter | Mencegah dip tidak realistis |
| Zona waktu | Konsisten dengan bujur atau dipilih sadar | Bujur Jawa dengan WIT akan menggeser hasil |
| Data ephemeris | Semua kolom wajib tersedia | Error lebih mudah diaudit sebelum hitung |
| Kriteria | Tinggi/elongasi tidak negatif kecuali sengaja | Menghindari konfigurasi absurd |

## Analisis Kemungkinan Perbedaan dengan Hitung Manual

Perbedaan antara aplikasi dan hitung manual bisa muncul dari hal-hal kecil berikut:

1. **Pembulatan setiap tahap.**
   Hitung manual sering membulatkan hasil sementara. Kode menyimpan presisi `Double` sampai akhir. Selisih kecil pada tahap awal dapat mempengaruhi tahap akhir.

2. **Format derajat-menit-detik.**
   Dokumen memakai DMS, kode menyimpan derajat desimal. Konversi balik ke DMS bisa menghasilkan beda detik busur karena pembulatan.

3. **Jam ghurub acuan awal.**
   Tahap 5 memakai jam acuan dari perkiraan ghurub lokal. Kode default memakai 18:00 lokal. Jika prosedur manual memakai jam lain, nilai semi diameter/deklinasi/equation of time awal dapat berbeda.

4. **Interpolasi sudut maju.**
   Kode menghindari loncatan 0/360. Manual yang tidak normalisasi dapat berbeda besar pada tanggal tertentu.

5. **Equation of time.**
   Kode mengambil equation of time pada jam acuan, bukan interpolasi saat ghurub. Jika manual menginterpolasi EoT, hasil ghurub dapat berbeda kecil.

6. **Refraksi.**
   Dokumen memberi rumus dan opsi daftar refraksi. Kode memakai rumus untuk `ho > 0` dan nilai rata-rata untuk `ho <= 0`. Jika auditor memakai tabel refraksi, hasil dapat berbeda.

7. **Tahap 30.**
   Perbedaan akar kuadrat adalah perbedaan formula yang paling jelas dan perlu dikonfirmasi.

8. **Kriteria kesimpulan.**
   Dokumen prosedur memuat perhitungan, sedangkan kode menambahkan evaluasi kriteria MABIMS/Kemenag. Ini berguna, tetapi harus dibedakan dari keputusan resmi.

## Skenario Perubahan Kebijakan atau Metode

Jika kebijakan awal bulan berubah, bagian kode yang terdampak:

| Perubahan | Bagian yang perlu diubah |
|---|---|
| Tinggi minimum berubah | `KriteriaAwalBulanFalak` |
| Elongasi minimum berubah | `KriteriaAwalBulanFalak` |
| Syarat ijtimak sebelum ghurub dihapus/ditambah | `KriteriaAwalBulanFalak.memakaiSyaratIjtimaSebelumGhurub` dan `susunKesimpulanHisabHilal` |
| Kriteria memakai umur bulan | Tambah perhitungan umur dari `saatIjtima` ke `saatGhurub` |
| Kriteria memakai elongasi topocentric | Tahap 32 perlu diganti/ditambah |
| Refraksi memakai tabel resmi | Tahap 17 perlu adapter tabel refraksi |
| Markaz harus resmi nasional | `FalakMarkazProvider` dan input UI perlu dibatasi |
| Data ephemeris berganti format | `FalakModels.kt`, `nilaiDerajat`, `nilaiJam`, `nilaiAngka` |

## Lampiran Edukasi: Alur Data dari Input ke Kesimpulan

Alur data dapat dibaca sebagai rantai sebab-akibat:

1. `Markaz + tanggal + kriteria` membentuk `KonteksHisabHilal`.
2. `KonteksHisabHilal` menentukan tanggal ephemeris yang harus dimuat.
3. Ephemeris menghasilkan FIB, ALB, ELM, Sd, deklinasi, EoT, AR, HP.
4. FIB/ALB/ELM menghasilkan ijtimak.
5. Markaz/Sd Matahari/deklinasi/EoT/KWD menghasilkan ghurub.
6. Ghurub menjadi waktu acuan interpolasi semua data Bulan dan Matahari.
7. Data Bulan/Matahari saat ghurub menghasilkan tinggi, azimut, FIB ghurub, dan elongasi.
8. Tinggi dan elongasi dibandingkan dengan kriteria.
9. Sistem menghasilkan prakiraan awal bulan.

Dengan membaca rantai ini, auditor bisa melacak setiap hasil akhir kembali ke input awal dan kolom ephemeris yang dipakai.

## Lampiran Edukasi: Pertanyaan Audit yang Harus Bisa Dijawab

Untuk setiap hasil hisab, dokumen audit sebaiknya memungkinkan auditor menjawab pertanyaan berikut:

- Data ephemeris tahun berapa dan versi apa yang dipakai?
- Tanggal situasi hilal yang dipakai apa?
- Markaz yang dipakai apa, dan sumbernya manual/GPS/peta/resmi?
- FIB terkecil terjadi pada jam UT berapa?
- ALB dan ELM diambil dari jam berapa?
- Apakah ada interpolasi yang melewati 0/360 derajat?
- Ghurub lokal dihitung jam berapa?
- Ghurub lokal setara jam UT berapa untuk interpolasi?
- Apakah tinggi hilal mar'i positif atau negatif?
- Apakah lama hilal positif atau negatif?
- Azimut yang dipakai dari titik Barat atau dari Utara?
- FIB ghurub dihitung dari kolom apa dan jam berapa?
- Elongasi yang dipakai geosentrik atau topocentric?
- Kriteria awal bulan apa yang dipakai?
- Apakah hasil memenuhi semua syarat aktif?
- Apakah ada nilai yang dekat ambang batas dan perlu pemeriksaan pembulatan?

## Sumber dan Ruang Lingkup

**Dokumen sumber yang dipakai:**

- `referensi/falaq/EPHEMERIS AWAL BULAN.docx`

**File Kotlin yang dipakai:**

- `app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt` - kalkulator inti 32 tahap dan pembentuk butir perhitungan.
- `app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalModels.kt` - model konteks, markaz, zona waktu, nilai ephemeris, hasil tiap tahap, dan kesimpulan.
- `app/src/main/java/com/alhasanah/alhasanahmedia/data/model/falak/FalakModels.kt` - struktur data ephemeris harian matahari/bulan.
- `app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/HisabHilalRepository.kt` - penyiapan data ephemeris dan pemanggilan kalkulator.
- `app/src/main/java/com/alhasanah/alhasanahmedia/ui/falak/HisabHilalViewModel.kt` - pembentukan konteks dari input pengguna.
- `app/src/main/java/com/alhasanah/alhasanahmedia/util/FalakMarkazProvider.kt` - dependency lokasi/markaz.
- `app/src/main/java/com/alhasanah/alhasanahmedia/di/AppModule.kt` - registrasi dependency.

**File yang diabaikan:**

- File UI umum (`ui/quran`, `ui/berita`, `ui/wallet`, `ui/santri`, `ui/auth`, `ui/theme`, dll.) diabaikan karena tidak berisi rumus perhitungan awal bulan.
- File gerhana (`GerhanaBulan*`, `JeanMeeusGerhanaBulanCalculator.kt`, `VisualGerhanaBulanMapper.kt`) diabaikan karena terkait gerhana bulan, bukan hisab hilal awal bulan.
- `VisualHilalMapper.kt` dan `VisualFalakCanvas.kt` diabaikan dari pemetaan rumus karena hanya memvisualisasikan hasil, bukan menghitung 32 tahap dokumen.
- Dokumen lain di `referensi/falaq` diabaikan karena sumber prosedur yang diminta untuk versi ini adalah `EPHEMERIS AWAL BULAN.docx`.

## Prasarana (Dependencies)

### Model konteks, zona waktu, dan kriteria

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalModels.kt
// Fungsi/Class: ZonaWaktuFalak, MarkazFalak, KonteksHisabHilal, KriteriaAwalBulanFalak
data class ZonaWaktuFalak(
    val nama: String,
    val offsetJam: Double,
    val bujurStandarDerajat: Double,
) {
    companion object {
        val WIB = ZonaWaktuFalak("WIB", 7.0, 105.0)
        val WITA = ZonaWaktuFalak("WITA", 8.0, 120.0)
        val WIT = ZonaWaktuFalak("WIT", 9.0, 135.0)
    }
}

data class MarkazFalak(
    val nama: String,
    val lintangDerajat: Double,
    val bujurDerajat: Double,
    val elevasiMeter: Double,
    val zonaWaktu: ZonaWaktuFalak = ZonaWaktuFalak.WIB,
)

data class KonteksHisabHilal(
    val bulanHijriah: String,
    val tanggalSituasiHilalMasehi: LocalDate,
    val markaz: MarkazFalak,
    val jamGhurubPerkiraanLokal: Double = 18.0,
    val kriteriaAwalBulan: KriteriaAwalBulanFalak = KriteriaAwalBulanFalak.KemenagMabimsTerbaru,
)

data class KriteriaAwalBulanFalak(
    val nama: String,
    val tinggiHilalMinimumDerajat: Double?,
    val elongasiMinimumDerajat: Double?,
    val memakaiSyaratIjtimaSebelumGhurub: Boolean = true,
) {
    companion object {
        val KemenagMabimsTerbaru = KriteriaAwalBulanFalak(
            nama = "Kemenag/MABIMS terbaru",
            tinggiHilalMinimumDerajat = 3.0,
            elongasiMinimumDerajat = 6.4,
        )

        val HisabWujudulHilal = KriteriaAwalBulanFalak(
            nama = "Hisab wujudul hilal",
            tinggiHilalMinimumDerajat = 0.0,
            elongasiMinimumDerajat = null,
        )

        val TanpaKriteria = KriteriaAwalBulanFalak(
            nama = "Tanpa kriteria visibilitas",
            tinggiHilalMinimumDerajat = null,
            elongasiMinimumDerajat = null,
            memakaiSyaratIjtimaSebelumGhurub = false,
        )
    }
}
```

### Model data ephemeris

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/model/falak/FalakModels.kt
// Fungsi/Class: FalakEphemerisHarian, FalakHourlyTable, helper JsonObject
@Serializable
data class FalakEphemerisHarian(
    val page: Int? = null,
    val date: String,
    @SerialName("has_structured_hourly_table") val hasStructuredHourlyTable: Boolean = false,
    @SerialName("hourly_table") val hourlyTable: FalakHourlyTable = FalakHourlyTable(),
    @SerialName("raw_text") val rawText: String? = null,
)

@Serializable
data class FalakHourlyTable(
    val sun: List<JsonObject> = emptyList(),
    val moon: List<JsonObject> = emptyList(),
)

fun JsonObject.textAt(name: String): String? = this[name]?.jsonPrimitiveContentOrNull()
fun JsonObject.numberAt(name: String): Double? = this[name]?.jsonNumberContentOrNull()
```

### Alur kalkulator utama

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitung
fun hitung(
    konteks: KonteksHisabHilal,
    ephemerisHarian: List<FalakEphemerisHarian>,
): HasilHisabHilalEphemeris {
    val ephemerisPerTanggal = ephemerisHarian.associateBy { LocalDate.parse(it.date) }
    val dataIjtima = tentukanDataIjtima(konteks, ephemerisPerTanggal)
    val sabaqIjtima = tentukanSabaqIjtima(dataIjtima)
    val saatIjtima = hitungSaatIjtima(konteks, dataIjtima, sabaqIjtima)
    val posisiMatahariHaqiqiGhurub = hitungPosisiMatahariHaqiqiGhurub(konteks, ephemerisPerTanggal)
    val sudutWaktuMatahariGhurub = hitungSudutWaktuMatahariGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        posisiMatahariHaqiqiGhurub = posisiMatahariHaqiqiGhurub
    )
    val koreksiWaktuDaerah = hitungKoreksiWaktuDaerah(konteks)
    val saatGhurub = hitungSaatGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        sudutWaktuMatahariGhurub = sudutWaktuMatahariGhurub,
        koreksiWaktuDaerah = koreksiWaktuDaerah
    )
    val asensiorektaMatahariGhurub = hitungAsensiorektaMatahariGhurub(konteks, ephemerisPerTanggal, saatGhurub)
    val asensiorektaBulanGhurub = hitungAsensiorektaBulanGhurub(konteks, ephemerisPerTanggal, saatGhurub)
    val sudutWaktuBulanGhurub = hitungSudutWaktuBulanGhurub(
        sudutWaktuMatahariGhurub = sudutWaktuMatahariGhurub,
        asensiorektaMatahariGhurub = asensiorektaMatahariGhurub,
        asensiorektaBulanGhurub = asensiorektaBulanGhurub
    )
    val deklinasiGhurub = hitungDeklinasiGhurub(konteks, ephemerisPerTanggal, saatGhurub)
    val tinggiBulanHaqiqiGhurub = hitungTinggiBulanHaqiqiGhurub(
        konteks = konteks,
        sudutWaktuBulanGhurub = sudutWaktuBulanGhurub,
        deklinasiGhurub = deklinasiGhurub
    )
```

Catatan: fungsi `hitung` melanjutkan alur sampai `hitungJarakBusurElongasi(...)` dan membentuk `butirPerhitungan` bernomor 1 sampai 32.

### Helper ephemeris, interpolasi, normalisasi, dan trigonometri derajat

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: interpolasiGhurub, interpolasiAngkaGhurub, normalisasi, trig derajat
private fun interpolasiGhurub(
    konteks: KonteksHisabHilal,
    ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
    saatGhurub: SaatGhurub,
    tabel: TabelEphemeris,
    kolom: String,
    mode: ModeInterpolasi,
): InterpolasiEphemerisFalak {
    val jamGhurubUt = saatGhurub.waktuLokal.jamDesimal - konteks.markaz.zonaWaktu.offsetJam
    val waktuGhurubUt = normalisasiWaktu(saatGhurub.waktuLokal.tanggal, jamGhurubUt, "GMT/UT")
    val jamAtasUt = floor(waktuGhurubUt.jamDesimal).toInt()
    val jamBawahUt = jamAtasUt + 1
    val nc = waktuGhurubUt.jamDesimal - jamAtasUt
    val barisAtas = barisEphemeris(ephemerisPerTanggal, waktuGhurubUt.tanggal, jamAtasUt, tabel)
    val barisBawah = barisEphemeris(ephemerisPerTanggal, waktuGhurubUt.tanggal, jamBawahUt, tabel)
    val nilaiAtas = nilaiDerajat(barisAtas.tanggal, barisAtas.jamUt, tabel, kolom, barisAtas.row)
    val nilaiBawah = nilaiDerajat(barisBawah.tanggal, barisBawah.jamUt, tabel, kolom, barisBawah.row)
    val hasil = when (mode) {
        ModeInterpolasi.LINEAR -> nilaiAtas.nilai - (nilaiAtas.nilai - nilaiBawah.nilai) * nc
        ModeInterpolasi.SUDUT_MAJU -> normalisasiDerajat(nilaiAtas.nilai + deltaMajuDerajat(nilaiAtas.nilai, nilaiBawah.nilai) * nc)
    }
    return InterpolasiEphemerisFalak(
        jamAtasUt = barisAtas.jamUt,
        jamBawahUt = barisBawah.jamUt,
        nc = nc,
        nilaiAtas = nilaiAtas,
        nilaiBawah = nilaiBawah,
        hasilDerajat = hasil,
    )
}

private fun deltaMajuDerajat(awal: Double, setelah: Double): Double {
    var delta = setelah - awal
    while (delta < 0.0) delta += 360.0
    return delta
}

private fun selisihSudutBertanda(nilaiKiri: Double, nilaiKanan: Double): Double {
    var delta = nilaiKiri - nilaiKanan
    while (delta > 180.0) delta -= 360.0
    while (delta <= -180.0) delta += 360.0
    return delta
}

private fun normalisasiDerajat(value: Double): Double {
    var normalized = value % 360.0
    if (normalized < 0.0) normalized += 360.0
    return normalized
}

private fun sinDeg(value: Double): Double = sin(value * PI / 180.0)

private fun cosDeg(value: Double): Double = cos(value * PI / 180.0)

private fun tanDeg(value: Double): Double = tan(value * PI / 180.0)
```

### Penyiapan ephemeris dan konteks

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/HisabHilalRepository.kt
// Fungsi: persiapkanEphemeris, hitung
override suspend fun persiapkanEphemeris(konteks: KonteksHisabHilal): Result<DataEphemerisHisabHilal> =
    runCatching {
        val tanggal = konteks.tanggalSituasiHilalMasehi
        val tanggalDiperlukan = setOf(tanggal, tanggal.plusDays(1))
        val paketPerTahun = tanggalDiperlukan
            .map { it.year }
            .distinct()
            .map { tahun ->
                falakRepository.loadDataLengkap(tahun).recoverCatching {
                    falakRepository.refreshPaketKemenag(tahun).getOrThrow()
                }.getOrThrow()
            }
        val ephemeris = paketPerTahun.flatMap { data ->
            data.ephemerisHarian.filter { item ->
                runCatching { LocalDate.parse(item.date) }.getOrNull() in tanggalDiperlukan
            }
        }
        val tersedia = ephemeris.mapTo(mutableSetOf()) { LocalDate.parse(it.date) }
        val hilang = tanggalDiperlukan - tersedia
        check(hilang.isEmpty()) {
            "Data ephemeris tanggal ${hilang.joinToString()} belum tersedia pada paket ${paketPerTahun.joinToString { it.paket.kode }}."
        }
        DataEphemerisHisabHilal(
            paketUtama = paketPerTahun.first(),
            paketPendukung = paketPerTahun.drop(1),
            tanggalSituasiHilalMasehi = tanggal,
            ephemerisHarian = ephemeris.distinctBy { it.date }.sortedBy { it.date },
        )
    }

override suspend fun hitung(konteks: KonteksHisabHilal): Result<HasilHisabHilalEphemeris> =
    runCatching {
        val siap = persiapkanEphemeris(konteks).getOrThrow()
        calculator.hitung(konteks, siap.ephemerisHarian)
    }
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/ui/falak/HisabHilalViewModel.kt
// Fungsi: buildKonteks
private fun buildKonteks(state: HisabHilalUiState): KonteksHisabHilal {
    val tanggal = LocalDate.parse(state.tanggalSituasiHilal.trim())
    val input = state.markazInput
    val lintang = input.lintang.toDoubleOrNull() ?: error("Lintang belum valid.")
    val bujur = input.bujur.toDoubleOrNull() ?: error("Bujur belum valid.")
    val elevasi = input.elevasi.toDoubleOrNull() ?: error("Elevasi belum valid.")
    val zona = when (input.zona.trim().uppercase()) {
        "WIB" -> ZonaWaktuFalak.WIB
        "WITA" -> ZonaWaktuFalak.WITA
        "WIT" -> ZonaWaktuFalak.WIT
        else -> FalakMarkazProvider.zonaWaktuIndonesia(bujur)
    }
    return KonteksHisabHilal(
        bulanHijriah = state.bulanHijriah.ifBlank { "Bulan Hijriah" },
        tanggalSituasiHilalMasehi = tanggal,
        markaz = MarkazFalak(
            nama = input.nama.ifBlank { "Markaz" },
            lintangDerajat = lintang,
            bujurDerajat = bujur,
            elevasiMeter = elevasi,
            zonaWaktu = zona,
        ),
        kriteriaAwalBulan = state.kriteria,
    )
}
```

## Tahap 1: Markaz

**Deskripsi:** Menentukan markaz perhitungan: nama tempat, lintang, bujur, elevasi, dan zona waktu.

**Rumus:** Input lokasi; tidak ada rumus numerik selain pemilihan koordinat dan zona.

**Kenapa kode ini dipakai:** `MarkazFalak` adalah titik awal seluruh perhitungan karena lintang, bujur, elevasi, dan zona waktu dipakai kembali pada tahap ghurub, tinggi hilal, azimut, KWD, dan dip. Tanpa model markaz yang eksplisit, hasil hisab tidak dapat diaudit ulang per lokasi.

**Kemungkinan penyesuaian:** Jika lembaga memakai markaz resmi tertentu, nilai `lintangDerajat`, `bujurDerajat`, dan `elevasiMeter` sebaiknya dikunci dari master data, bukan hanya input manual/GPS.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalModels.kt
// Class: MarkazFalak
data class MarkazFalak(
    val nama: String,
    val lintangDerajat: Double,
    val bujurDerajat: Double,
    val elevasiMeter: Double,
    val zonaWaktu: ZonaWaktuFalak = ZonaWaktuFalak.WIB,
)
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: tampilkanMarkaz
ButirPerhitunganFalak(
    nomor = 1,
    judul = "Markaz",
    rumus = "Markaz = nama tempat, lintang (phi), bujur (lambda), dan tinggi tempat/elevasi.",
    substitusi = "${konteks.markaz.nama}; phi=${formatDerajat(konteks.markaz.lintangDerajat)}, lambda=${formatDerajat(konteks.markaz.bujurDerajat)}, elevasi=${formatAngka(konteks.markaz.elevasiMeter)} m",
    hasil = "${konteks.markaz.nama}, ${konteks.markaz.zonaWaktu.nama}",
    catatan = "Nilai ini berasal dari input pengguna, pilihan peta, GPS, atau markaz tersimpan."
)
```

## Tahap 2: Data Ijtimak FIB, ALB, dan ELM

**Deskripsi:** Mengambil FIB terkecil, apparent longitude Bulan pada jam FIB dan jam berikutnya, serta ecliptic longitude Matahari pada jam yang sama.

**Rumus:** Ambil data ephemeris: `FIB minimum`, `ALB_jam`, `ALB_jam+1`, `ELM_jam`, `ELM_jam+1`.

**Kenapa kode ini dipakai:** Dokumen menjadikan FIB terkecil sebagai titik acuan awal untuk mendekati ijtimak. Kode mencari baris Bulan dengan `fraction_illumination_percent` paling kecil, lalu mengambil ALB dan ELM pada jam tersebut dan jam setelahnya sebagai bahan interpolasi ijtimak.

**Kemungkinan penyesuaian:** Jika data ephemeris tidak menyertakan FIB atau jika FIB minimum terjadi dekat batas tanggal, perlu memastikan data tanggal sebelum/sesudah tersedia. Repository saat ini hanya menyiapkan tanggal situasi hilal dan tanggal setelahnya.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: tentukanDataIjtima
val barisFibTerkecil = dataTanggal.hourlyTable.moon.minByOrNull { row ->
    row.doubleAt("fraction_illumination_percent") ?: Double.POSITIVE_INFINITY
} ?: error("Data Bulan tanggal $tanggal kosong.")
val jamFibUt = barisFibTerkecil.hourUt()
val bulanJamSetelahnya = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt + 1, TabelEphemeris.BULAN)
val matahariJamFib = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt, TabelEphemeris.MATAHARI)
val matahariJamSetelahnya = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt + 1, TabelEphemeris.MATAHARI)

return DataIjtima(
    fibTerkecilPersen = nilaiAngka(tanggal, jamFibUt, TabelEphemeris.BULAN, "fraction_illumination_percent", barisFibTerkecil),
    jamFibUt = jamFibUt,
    albJamFib = nilaiDerajat(tanggal, jamFibUt, TabelEphemeris.BULAN, "apparent_longitude", barisFibTerkecil),
    albJamSetelahnya = nilaiDerajat(bulanJamSetelahnya.tanggal, bulanJamSetelahnya.jamUt, TabelEphemeris.BULAN, "apparent_longitude", bulanJamSetelahnya.row),
    elmJamFib = nilaiDerajat(tanggal, jamFibUt, TabelEphemeris.MATAHARI, "apparent_ecliptic_longitude", matahariJamFib.row),
    elmJamSetelahnya = nilaiDerajat(matahariJamSetelahnya.tanggal, matahariJamSetelahnya.jamUt, TabelEphemeris.MATAHARI, "apparent_ecliptic_longitude", matahariJamSetelahnya.row),
)
```

## Tahap 3: Sabaq Bulan dan Sabaq Matahari

**Deskripsi:** Menghitung selisih gerak Bulan dan Matahari antara dua jam ephemeris sekitar FIB terkecil.

**Rumus:** `SB = ALB(jam+1) - ALB(jam)`, `SM = ELM(jam+1) - ELM(jam)`.

**Kenapa kode ini dipakai:** Sabaq adalah kecepatan relatif per jam yang dipakai untuk mencari titik saat longitude Matahari dan Bulan sejajar. Kode memakai `deltaMajuDerajat` agar selisih tetap positif ketika longitude melewati 360 derajat.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: tentukanSabaqIjtima
private fun tentukanSabaqIjtima(dataIjtima: DataIjtima): SabaqIjtima =
    SabaqIjtima(
        sabaqBulanDerajat = deltaMajuDerajat(dataIjtima.albJamFib.nilai, dataIjtima.albJamSetelahnya.nilai),
        sabaqMatahariDerajat = deltaMajuDerajat(dataIjtima.elmJamFib.nilai, dataIjtima.elmJamSetelahnya.nilai),
    )
```

**Catatan perbedaan:** Dokumen menulis selisih sederhana. Implementasi memakai `deltaMajuDerajat(...)` agar aman saat longitude melewati 0/360 derajat.

## Tahap 4: Saat Ijtimak

**Deskripsi:** Menghitung waktu ijtimak berdasarkan jarak longitude Matahari-Bulan dan sabaq relatif.

**Rumus:** `Ijtimak = Jam FIB + (ELM - ALB) / (SB - SM) + offset zona waktu`.

**Kenapa kode ini dipakai:** Formula dokumen dihitung dalam GMT/UT lalu dikonversi ke waktu lokal. Kode memisahkan `waktuUt` dan `waktuLokal`, sehingga audit dapat melihat nilai sebelum dan sesudah konversi zona waktu.

**Kemungkinan penyesuaian:** Jika audit lembaga menghendaki istilah "GMT" tetap dipakai, label `"GMT/UT"` pada kode bisa disesuaikan di layer tampilan. Secara perhitungan, keduanya dipakai sebagai acuan tabel ephemeris.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungSaatIjtima
val jarakElmAlb = selisihSudutBertanda(dataIjtima.elmJamFib.nilai, dataIjtima.albJamFib.nilai)
val jamIjtimaUt = dataIjtima.jamFibUt + (
    jarakElmAlb / (sabaqIjtima.sabaqBulanDerajat - sabaqIjtima.sabaqMatahariDerajat)
    )
return SaatIjtima(
    jarakElmAlbDerajat = jarakElmAlb,
    waktuUt = normalisasiWaktu(konteks.tanggalSituasiHilalMasehi, jamIjtimaUt, "GMT/UT"),
    waktuLokal = normalisasiWaktu(
        konteks.tanggalSituasiHilalMasehi,
        jamIjtimaUt + konteks.markaz.zonaWaktu.offsetJam,
        konteks.markaz.zonaWaktu.nama
    ),
)
```

**Catatan perbedaan:** Dokumen memberi contoh `+ 7 jam` untuk WIB. Implementasi memakai `konteks.markaz.zonaWaktu.offsetJam`, sehingga berlaku untuk WIB/WITA/WIT.

## Tahap 5: Posisi Matahari Haqiqi pada Ghurub

**Deskripsi:** Menghitung tinggi Matahari haqiqi saat ghurub dengan koreksi semi diameter, refraksi rata-rata, dan dip.

**Rumus:** `h_matahari = 0 - Sd_matahari - refraksi_ghurub - dip`, `dip = sqrt(elevasi) * 0.0293`.

**Kenapa kode ini dipakai:** Tahap ini menentukan tinggi Matahari yang menjadi input sudut waktu Matahari. Nilai `jamAcuanUt` dihitung dari perkiraan ghurub lokal dikurangi offset zona, sesuai keterangan dokumen bahwa jam 18 WIB setara jam 11 GMT.

**Kemungkinan penyesuaian:** Kode memakai `jamGhurubPerkiraanLokal = 18.0` sebagai acuan awal. Untuk daerah ekstrem atau kebutuhan presisi tinggi, acuan awal dapat dibuat adaptif, misalnya dari perkiraan astronomis atau jadwal magrib.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungPosisiMatahariHaqiqiGhurub
val jamAcuanUt = floor(konteks.jamGhurubPerkiraanLokal - konteks.markaz.zonaWaktu.offsetJam).toInt()
val semiDiameterMatahari = nilaiDerajat(
    matahariGhurub.tanggal,
    matahariGhurub.jamUt,
    TabelEphemeris.MATAHARI,
    "semi_diameter",
    matahariGhurub.row
)
val refraksiGhurub = 34.5 / 60.0
val dip = sqrt(konteks.markaz.elevasiMeter.coerceAtLeast(0.0)) * 0.0293
return PosisiMatahariHaqiqiGhurub(
    jamAcuanUt = jamAcuanUt,
    semiDiameterMatahariDerajat = semiDiameterMatahari,
    refraksiGhurubDerajat = refraksiGhurub,
    dipDerajat = dip,
    tinggiMatahariHaqiqiDerajat = 0.0 - semiDiameterMatahari.nilai - refraksiGhurub - dip,
)
```

## Tahap 6: Sudut Waktu Matahari pada Ghurub

**Deskripsi:** Menghitung sudut waktu Matahari saat ghurub.

**Rumus:** `t_matahari = arccos(-tan(phi) tan(d_matahari) + sin(h_matahari) / cos(phi) / cos(d_matahari))`.

**Kenapa kode ini dipakai:** Rumus ini menentukan jarak waktu Matahari dari meridian saat terbenam. Kode memakai fungsi trigonometri derajat (`sinDeg`, `cosDeg`, `tanDeg`) agar formula sesuai satuan dokumen.

**Kemungkinan penyesuaian:** `argumenCosinus.coerceIn(-1.0, 1.0)` mencegah error floating point. Dalam audit numerik, nilai sebelum `coerceIn` tetap penting untuk dicatat jika mendekati batas.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungSudutWaktuMatahariGhurub
val argumenCosinus = -tanDeg(lintang) * tanDeg(deklinasi) +
    (sinDeg(tinggi) / cosDeg(lintang) / cosDeg(deklinasi))
return SudutWaktuMatahariGhurub(
    jamAcuanUt = posisiMatahariHaqiqiGhurub.jamAcuanUt,
    deklinasiMatahariDerajat = deklinasiMatahari,
    tinggiMatahariHaqiqiDerajat = tinggi,
    argumenCosinus = argumenCosinus,
    sudutWaktuDerajat = acos(argumenCosinus.coerceIn(-1.0, 1.0)) * 180.0 / PI,
)
```

## Tahap 7: Koreksi Waktu Daerah

**Deskripsi:** Menghitung koreksi waktu berdasarkan selisih bujur standar zona dan bujur markaz.

**Rumus:** `KWD = (bujur standar - bujur markaz) / 15`.

**Kenapa kode ini dipakai:** KWD mengoreksi perbedaan bujur markaz terhadap meridian standar zona waktu. Ini membuat ghurub lokal tidak hanya bergantung pada offset WIB/WITA/WIT, tetapi juga posisi bujur aktual.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungKoreksiWaktuDaerah
private fun hitungKoreksiWaktuDaerah(konteks: KonteksHisabHilal): KoreksiWaktuDaerah =
    KoreksiWaktuDaerah(
        bujurStandarDerajat = konteks.markaz.zonaWaktu.bujurStandarDerajat,
        bujurMarkazDerajat = konteks.markaz.bujurDerajat,
        koreksiJam = (konteks.markaz.zonaWaktu.bujurStandarDerajat - konteks.markaz.bujurDerajat) / 15.0,
    )
```

## Tahap 8: Saat Ghurub

**Deskripsi:** Menghitung waktu Matahari terbenam lokal.

**Rumus:** `Ghurub = (t_matahari / 15) + (12 - equation_of_time) + KWD`.

**Kenapa kode ini dipakai:** Tahap ini menghasilkan waktu ghurub lokal yang menjadi titik interpolasi untuk banyak data berikutnya. Jika ghurub salah, tahap 9 sampai 32 akan ikut bergeser.

**Kemungkinan penyesuaian:** `equation_of_time` diambil pada `jamAcuanUt` yang sama dengan tahap 5/6. Jika prosedur lembaga mengharuskan interpolasi equation of time pada ghurub, bagian ini perlu disesuaikan.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungSaatGhurub
val jamGhurub = (sudutWaktuMatahariGhurub.sudutWaktuDerajat / 15.0) +
    (12.0 - equationOfTime.nilai) +
    koreksiWaktuDaerah.koreksiJam
return SaatGhurub(
    sudutWaktuMatahariDerajat = sudutWaktuMatahariGhurub.sudutWaktuDerajat,
    equationOfTimeJam = equationOfTime,
    koreksiWaktuDaerahJam = koreksiWaktuDaerah.koreksiJam,
    waktuLokal = normalisasiWaktu(konteks.tanggalSituasiHilalMasehi, jamGhurub, konteks.markaz.zonaWaktu.nama),
)
```

## Tahap 9: Asensiorekta Matahari pada Ghurub

**Deskripsi:** Menginterpolasi apparent right ascension Matahari pada waktu ghurub.

**Rumus:** `AR_matahari = Na - (Na - Nb) * Nc`.

**Kenapa kode ini dipakai:** Data ephemeris tersedia per jam, sedangkan ghurub hampir selalu berada di antara dua jam UT. Karena itu nilai AR Matahari harus diinterpolasi pada pecahan waktu ghurub.

**Kemungkinan penyesuaian:** Kode memakai interpolasi sudut maju. Bila sumber ephemeris menyimpan right ascension dalam jam, bukan derajat, konversi dan nama kolom harus diverifikasi.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungAsensiorektaMatahariGhurub
AsensiorektaMatahariGhurub(
    interpolasi = interpolasiGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        saatGhurub = saatGhurub,
        tabel = TabelEphemeris.MATAHARI,
        kolom = "apparent_right_ascension",
        mode = ModeInterpolasi.SUDUT_MAJU
    )
)
```

**Catatan perbedaan:** Untuk right ascension, implementasi memakai mode `SUDUT_MAJU`, bukan linear murni, untuk menangani transisi 0/360 derajat.

## Tahap 10: Asensiorekta Bulan pada Ghurub

**Deskripsi:** Menginterpolasi apparent right ascension Bulan pada waktu ghurub.

**Rumus:** `AR_bulan = Na - (Na - Nb) * Nc`.

**Kenapa kode ini dipakai:** AR Bulan diperlukan untuk sudut waktu Bulan dan elongasi. Karena gerak Bulan relatif cepat, interpolasi pada waktu ghurub penting untuk menghindari kesalahan posisi.

**Kemungkinan penyesuaian:** Sama seperti tahap 9, mode sudut maju dipakai untuk menghindari diskontinuitas 0/360 derajat.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungAsensiorektaBulanGhurub
AsensiorektaBulanGhurub(
    interpolasi = interpolasiGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        saatGhurub = saatGhurub,
        tabel = TabelEphemeris.BULAN,
        kolom = "apparent_right_ascension",
        mode = ModeInterpolasi.SUDUT_MAJU
    )
)
```

**Catatan perbedaan:** Sama seperti tahap 9, right ascension Bulan memakai interpolasi sudut maju.

## Tahap 11: Sudut Waktu Bulan pada Ghurub

**Deskripsi:** Menghitung sudut waktu Bulan berdasarkan selisih asensiorekta Matahari-Bulan dan sudut waktu Matahari.

**Rumus:** `t_bulan = (AR_matahari - AR_bulan) + t_matahari`.

**Kenapa kode ini dipakai:** Sudut waktu Bulan tidak dihitung langsung dari waktu sipil, tetapi diturunkan dari perbedaan AR Matahari-Bulan dan sudut waktu Matahari. Ini sesuai struktur dokumen dan menjaga konsistensi dengan hasil ghurub.

**Kemungkinan penyesuaian:** Jika selisih AR tidak dinormalisasi, hasil dapat salah saat AR melewati batas 0/360 derajat. Kode sudah mengantisipasi dengan `selisihSudutBertanda`.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungSudutWaktuBulanGhurub
val arMatahari = asensiorektaMatahariGhurub.interpolasi.hasilDerajat
val arBulan = asensiorektaBulanGhurub.interpolasi.hasilDerajat
return SudutWaktuBulanGhurub(
    asensiorektaMatahariDerajat = arMatahari,
    asensiorektaBulanDerajat = arBulan,
    sudutWaktuMatahariDerajat = sudutWaktuMatahariGhurub.sudutWaktuDerajat,
    sudutWaktuBulanDerajat = selisihSudutBertanda(arMatahari, arBulan) + sudutWaktuMatahariGhurub.sudutWaktuDerajat,
)
```

## Tahap 12: Deklinasi Matahari dan Bulan pada Ghurub

**Deskripsi:** Menginterpolasi apparent declination Matahari dan Bulan pada waktu ghurub.

**Rumus:** `D = Na - (Na - Nb) * Nc`.

**Kenapa kode ini dipakai:** Deklinasi Matahari dan Bulan pada waktu ghurub dipakai untuk tinggi Bulan haqiqi, azimut, arah terbenam, dan elongasi. Karena waktunya di antara dua jam tabel, interpolasi linear dipakai.

**Kemungkinan penyesuaian:** Untuk interval satu jam, interpolasi linear umumnya memadai. Untuk audit presisi tinggi, hasil dapat dibandingkan dengan interpolasi orde lebih tinggi dari sumber ephemeris.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungDeklinasiGhurub
DeklinasiGhurub(
    matahari = interpolasiGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        saatGhurub = saatGhurub,
        tabel = TabelEphemeris.MATAHARI,
        kolom = "apparent_declination",
        mode = ModeInterpolasi.LINEAR
    ),
    bulan = interpolasiGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        saatGhurub = saatGhurub,
        tabel = TabelEphemeris.BULAN,
        kolom = "apparent_declination",
        mode = ModeInterpolasi.LINEAR
    )
)
```

## Tahap 13: Tinggi Bulan Haqiqi pada Ghurub

**Deskripsi:** Menghitung tinggi Bulan haqiqi saat ghurub.

**Rumus:** `h_bulan = arcsin(sin(phi) sin(d_bulan) + cos(phi) cos(d_bulan) cos(t_bulan))`.

**Kenapa kode ini dipakai:** Ini rumus koordinat horizon standar untuk menghitung altitude Bulan dari lintang, deklinasi, dan sudut waktu. Hasilnya menjadi dasar koreksi parallax, refraksi, dan tinggi hilal mar'i.

**Kemungkinan penyesuaian:** Argumen sinus dikunci ke rentang valid `[-1, 1]`. Jika nilai mentah sering melewati rentang karena kesalahan data, perlu audit data input, bukan hanya menerima hasil clamp.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungTinggiBulanHaqiqiGhurub
val argumenSinus = sinDeg(lintang) * sinDeg(deklinasiBulan) +
    cosDeg(lintang) * cosDeg(deklinasiBulan) * cosDeg(sudutWaktuBulan)
return TinggiBulanHaqiqiGhurub(
    lintangMarkazDerajat = lintang,
    deklinasiBulanDerajat = deklinasiBulan,
    sudutWaktuBulanDerajat = sudutWaktuBulan,
    argumenSinus = argumenSinus,
    tinggiBulanHaqiqiDerajat = kotlin.math.asin(argumenSinus.coerceIn(-1.0, 1.0)) * 180.0 / PI,
)
```

## Tahap 14: Parallax Bulan

**Deskripsi:** Menghitung parallax Bulan pada waktu ghurub dari horizontal parallax dan tinggi Bulan haqiqi.

**Rumus:** `Parallax = HP * cos(h_bulan)`.

**Kenapa kode ini dipakai:** Horizontal parallax dari tabel ephemeris dikoreksi terhadap tinggi Bulan. Tahap ini penting karena hilal dekat ufuk sangat sensitif terhadap parallax.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungParallaxBulanGhurub
val hp = interpolasiGhurub(
    konteks = konteks,
    ephemerisPerTanggal = ephemerisPerTanggal,
    saatGhurub = saatGhurub,
    tabel = TabelEphemeris.BULAN,
    kolom = "horizontal_parallax",
    mode = ModeInterpolasi.LINEAR
)
return ParallaxBulanGhurub(
    horizontalParallax = hp,
    tinggiBulanHaqiqiDerajat = tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat,
    parallaxDerajat = hp.hasilDerajat * cosDeg(tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat),
)
```

## Tahap 15: Semi Diameter Bulan

**Deskripsi:** Menginterpolasi semi diameter Bulan pada waktu ghurub.

**Rumus:** `Sd_bulan = Na - (Na - Nb) * Nc`.

**Kenapa kode ini dipakai:** Semi diameter Bulan diperlukan untuk koreksi tepi atas hilal. Kode mengambil nilai pada waktu ghurub melalui interpolasi linear.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungSemiDiameterBulanGhurub
SemiDiameterBulanGhurub(
    interpolasi = interpolasiGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        saatGhurub = saatGhurub,
        tabel = TabelEphemeris.BULAN,
        kolom = "semi_diameter",
        mode = ModeInterpolasi.LINEAR
    )
)
```

## Tahap 16: ho Bulan

**Deskripsi:** Menghitung tinggi geosentrik terkoreksi tepi atas sebelum refraksi/dip.

**Rumus:** `ho = h_bulan - parallax + Sd_bulan`.

**Kenapa kode ini dipakai:** Tahap ini menggabungkan altitude haqiqi, parallax, dan semi diameter untuk mendapatkan posisi tepi atas sebelum refraksi dan dip.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungHoBulanGhurub
HoBulanGhurub(
    tinggiBulanHaqiqiDerajat = tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat,
    parallaxDerajat = parallaxBulanGhurub.parallaxDerajat,
    semiDiameterBulanDerajat = semiDiameterBulanGhurub.interpolasi.hasilDerajat,
    hoDerajat = tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat -
        parallaxBulanGhurub.parallaxDerajat +
        semiDiameterBulanGhurub.interpolasi.hasilDerajat,
)
```

## Tahap 17: Refraksi Hilal

**Deskripsi:** Menghitung refraksi; bila `ho <= 0`, memakai refraksi rata-rata.

**Rumus:** Jika `ho <= 0`, `refraksi = 0°34'30"`. Jika `ho > 0`, `refraksi = 0.0167 / tan(ho + 7.31 / (ho + 4.4))`.

**Kenapa kode ini dipakai:** Dokumen memberi dua kondisi: refraksi rata-rata untuk tinggi hilal nol/negatif dan rumus refraksi untuk tinggi positif. Kode menerapkan percabangan tersebut secara eksplisit.

**Kemungkinan penyesuaian:** Refraksi sangat dipengaruhi atmosfer. Jika audit lembaga memakai tabel refraksi tertentu, rumus ini dapat berbeda dari tabel dan perlu diselaraskan.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungRefraksiHilal
val refraksi = if (ho <= 0.0) {
    34.5 / 60.0
} else {
    0.0167 / tanDeg(ho + 7.31 / (ho + 4.4))
}
return RefraksiHilal(
    hoDerajat = ho,
    refraksiDerajat = refraksi,
    menggunakanRefraksiRataRata = ho <= 0.0,
)
```

## Tahap 18: Tinggi Bulan Mar'i

**Deskripsi:** Menghitung tinggi hilal mar'i tepi atas dengan refraksi dan dip.

**Rumus:** `h'_bulan = ho + refraksi + dip`.

**Kenapa kode ini dipakai:** Tinggi hilal mar'i adalah salah satu output utama dan dipakai dalam kriteria awal bulan. Kode memakai bentuk ringkas dokumen: `ho + refraksi + dip`.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungTinggiBulanMariGhurub
TinggiBulanMariGhurub(
    hoDerajat = hoBulanGhurub.hoDerajat,
    refraksiDerajat = refraksiHilal.refraksiDerajat,
    dipDerajat = posisiMatahariHaqiqiGhurub.dipDerajat,
    tinggiBulanMariDerajat = hoBulanGhurub.hoDerajat +
        refraksiHilal.refraksiDerajat +
        posisiMatahariHaqiqiGhurub.dipDerajat,
)
```

## Tahap 19: Nishful Fadhlah Bulan

**Deskripsi:** Menghitung nishful fadhlah Bulan dari lintang tempat dan deklinasi Bulan.

**Rumus:** `NF = arcsin((sin(phi) sin(d_bulan)) / (cos(phi) cos(d_bulan)))`.

**Kenapa kode ini dipakai:** NF menjadi komponen setengah busur siang Bulan. Rumusnya bergantung pada lintang markaz dan deklinasi Bulan hasil interpolasi.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungNishfulFadhlahBulan
val argumenSinus = (sinDeg(lintang) * sinDeg(deklinasiBulan)) / (cosDeg(lintang) * cosDeg(deklinasiBulan))
return NishfulFadhlahBulan(
    lintangMarkazDerajat = lintang,
    deklinasiBulanDerajat = deklinasiBulan,
    argumenSinus = argumenSinus,
    nfDerajat = kotlin.math.asin(argumenSinus.coerceIn(-1.0, 1.0)) * 180.0 / PI,
)
```

## Tahap 20: Parallax Nishful Fadhlah

**Deskripsi:** Menghitung parallax nishful fadhlah.

**Rumus:** `PNF = cos(NF) * HP`.

**Kenapa kode ini dipakai:** PNF mengoreksi setengah busur siang Bulan terhadap parallax. Kode memakai horizontal parallax hasil interpolasi dari tahap 14, bukan parallax terkoreksi altitude.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungParallaxNishfulFadhlah
ParallaxNishfulFadhlah(
    nfDerajat = nishfulFadhlahBulan.nfDerajat,
    horizontalParallaxDerajat = parallaxBulanGhurub.horizontalParallax.hasilDerajat,
    pnfDerajat = cosDeg(nishfulFadhlahBulan.nfDerajat) * parallaxBulanGhurub.horizontalParallax.hasilDerajat,
)
```

## Tahap 21: Setengah Busur Siang Bulan Haqiqi

**Deskripsi:** Menghitung setengah busur siang Bulan haqiqi.

**Rumus:** `SBSH = 90 + NF`.

**Kenapa kode ini dipakai:** Ini tahap antara untuk menentukan cabang rumus SBS pada tahap 22.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungSetengahBusurSiangBulanHaqiqi
SetengahBusurSiangBulanHaqiqi(
    nfDerajat = nishfulFadhlahBulan.nfDerajat,
    sbshDerajat = 90.0 + nishfulFadhlahBulan.nfDerajat,
)
```

## Tahap 22: Setengah Busur Siang Bulan

**Deskripsi:** Menghitung setengah busur siang Bulan dengan koreksi PNF, semi diameter, refraksi rata-rata, dan dip.

**Rumus:** Jika `SBSH > 90`: `SBS = 90 + NF - PNF + (SD + 0.575 + dip)`. Jika `SBSH < 90`: `SBS = 90 + NF + PNF - (SD + 0.575 + dip)`.

**Kenapa kode ini dipakai:** Dokumen memberikan dua cabang berdasarkan SBSH. Kode menerapkan cabang tersebut dengan `if (sbsh > 90.0)`.

**Kemungkinan penyesuaian:** Dokumen hanya menyebut `lebih dari 90` dan `kurang dari 90`. Jika `SBSH == 90`, kode masuk cabang `else` setara cabang kurang dari/sama dengan 90. Ini perlu dikonfirmasi bila kasus tepat 90 derajat dianggap khusus.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungSetengahBusurSiangBulan
val sd = semiDiameterBulanGhurub.interpolasi.hasilDerajat
val refraksiRataRata = 34.5 / 60.0
val dip = posisiMatahariHaqiqiGhurub.dipDerajat
val koreksiTepiAtas = sd + refraksiRataRata + dip
val sbsh = setengahBusurSiangBulanHaqiqi.sbshDerajat
val sbs = if (sbsh > 90.0) {
    90.0 + nishfulFadhlahBulan.nfDerajat - parallaxNishfulFadhlah.pnfDerajat + koreksiTepiAtas
} else {
    90.0 + nishfulFadhlahBulan.nfDerajat + parallaxNishfulFadhlah.pnfDerajat - koreksiTepiAtas
}
```

## Tahap 23: Lama Hilal / Mukuts

**Deskripsi:** Menghitung durasi hilal di atas ufuk setelah ghurub.

**Rumus:** `LM = (SBS - t_bulan) / 15`.

**Kenapa kode ini dipakai:** Selisih busur antara SBS dan sudut waktu Bulan dikonversi ke jam dengan pembagi 15 derajat per jam. Hasil positif berarti hilal masih berada di atas ufuk setelah ghurub; hasil negatif berarti hilal sudah terbenam sebelum ghurub.

**Kemungkinan penyesuaian:** Tampilan hasil perlu jelas bila `lamaHilalJam` negatif, karena secara edukasi berarti "hilal terbenam sebelum Matahari", bukan durasi terlihat.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungLamaHilalMukuts
LamaHilalMukuts(
    sbsDerajat = setengahBusurSiangBulan.sbsDerajat,
    sudutWaktuBulanDerajat = sudutWaktuBulanGhurub.sudutWaktuBulanDerajat,
    lamaHilalJam = (setengahBusurSiangBulan.sbsDerajat - sudutWaktuBulanGhurub.sudutWaktuBulanDerajat) / 15.0,
)
```

## Tahap 24: Terbenam Hilal

**Deskripsi:** Menghitung waktu terbenam hilal.

**Rumus:** `TRB = ghurub + LM`.

**Kenapa kode ini dipakai:** Waktu terbenam hilal adalah output praktis dari lama hilal. Kode memakai `normalisasiWaktu` agar hasil tetap benar bila melewati tengah malam atau bernilai negatif.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungTerbenamHilal
TerbenamHilal(
    ghurub = saatGhurub.waktuLokal,
    lamaHilalJam = lamaHilalMukuts.lamaHilalJam,
    waktuLokal = normalisasiWaktu(
        saatGhurub.waktuLokal.tanggal,
        saatGhurub.waktuLokal.jamDesimal + lamaHilalMukuts.lamaHilalJam,
        saatGhurub.waktuLokal.zona
    ),
)
```

## Tahap 25: Azimut Matahari

**Deskripsi:** Menghitung azimut Matahari dari titik Barat saat ghurub.

**Rumus:** `Az_matahari = atan(-sin(phi)/tan(t_matahari) + cos(phi) tan(d_matahari)/sin(t_matahari))`.

**Kenapa kode ini dipakai:** Azimut Matahari menjadi acuan posisi hilal relatif terhadap Matahari. Dokumen memakai arah dari titik Barat ke utara/selatan; kode menyimpan tanda hasil untuk menentukan arah.

**Kemungkinan penyesuaian:** Jika UI membutuhkan azimut kompas 0-360 derajat dari utara, hasil dari titik Barat ini perlu dikonversi dan diberi label agar tidak tertukar.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungAzimutMatahariGhurub
val argumenTangen = (-sinDeg(lintang) / tanDeg(sudutWaktu)) +
    (cosDeg(lintang) * tanDeg(deklinasi) / sinDeg(sudutWaktu))
val azimut = atan(argumenTangen) * 180.0 / PI
return AzimutMatahariGhurub(
    lintangMarkazDerajat = lintang,
    sudutWaktuMatahariDerajat = sudutWaktu,
    deklinasiMatahariDerajat = deklinasi,
    argumenTangen = argumenTangen,
    azimutDerajat = azimut,
    arahDariBarat = arahAzimutDariBarat(azimut),
)
```

## Tahap 26: Azimut Bulan

**Deskripsi:** Menghitung azimut Bulan dari titik Barat saat ghurub.

**Rumus:** `Az_bulan = atan(-sin(phi)/tan(t_bulan) + cos(phi) tan(d_bulan)/sin(t_bulan))`.

**Kenapa kode ini dipakai:** Azimut Bulan dibandingkan dengan azimut Matahari untuk menentukan posisi hilal relatif pada tahap 27.

**Kemungkinan penyesuaian:** Sama seperti tahap 25, konvensi azimut "dari titik Barat" harus dipertahankan dalam audit agar tidak dicampur dengan azimut kompas.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungAzimutBulanGhurub
val argumenTangen = (-sinDeg(lintang) / tanDeg(sudutWaktu)) +
    (cosDeg(lintang) * tanDeg(deklinasi) / sinDeg(sudutWaktu))
val azimut = atan(argumenTangen) * 180.0 / PI
return AzimutBulanGhurub(
    lintangMarkazDerajat = lintang,
    sudutWaktuBulanDerajat = sudutWaktu,
    deklinasiBulanDerajat = deklinasi,
    argumenTangen = argumenTangen,
    azimutDerajat = azimut,
    arahDariBarat = arahAzimutDariBarat(azimut),
)
```

## Tahap 27: Posisi Hilal

**Deskripsi:** Menentukan posisi hilal relatif terhadap Matahari.

**Rumus:** `PH = Az_bulan - Az_matahari`.

**Kenapa kode ini dipakai:** Selisih azimut menentukan hilal berada di utara atau selatan Matahari sesuai dokumen. Nilai ini juga dipakai untuk nurul hilal dan kemiringan.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungPosisiHilal
val posisi = azimutBulanGhurub.azimutDerajat - azimutMatahariGhurub.azimutDerajat
return PosisiHilal(
    azimutBulanDerajat = azimutBulanGhurub.azimutDerajat,
    azimutMatahariDerajat = azimutMatahariGhurub.azimutDerajat,
    posisiHilalDerajat = posisi,
    arahDariMatahari = if (posisi >= 0.0) "utara Matahari" else "selatan Matahari",
)
```

## Tahap 28: Arah Terbenam Hilal

**Deskripsi:** Menghitung arah terbenam hilal.

**Rumus:** `AT = atan(-sin(phi)/tan(SBS) + cos(phi) tan(d_bulan)/sin(SBS))`.

**Kenapa kode ini dipakai:** Arah terbenam hilal dihitung dari SBS, lintang, dan deklinasi Bulan. Tahap ini adalah output kesimpulan tambahan yang membantu interpretasi arah hilal.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungArahTerbenamHilal
val argumenTangen = (-sinDeg(lintang) / tanDeg(sbs)) +
    (cosDeg(lintang) * tanDeg(deklinasi) / sinDeg(sbs))
return ArahTerbenamHilal(
    lintangMarkazDerajat = lintang,
    sbsDerajat = sbs,
    deklinasiBulanDerajat = deklinasi,
    argumenTangen = argumenTangen,
    arahTerbenamDerajat = atan(argumenTangen) * 180.0 / PI,
)
```

## Tahap 29: Luas Cahaya Hilal / Fraction Illumination

**Deskripsi:** Menginterpolasi FIB pada saat ghurub.

**Rumus:** `FIB_ghurub = Na - (Na - Nb) * Nc`.

**Kenapa kode ini dipakai:** FIB pada waktu ghurub adalah indikator iluminasi hilal, berbeda dari FIB minimum yang dipakai pada tahap 2 untuk mendekati ijtimak.

**Kemungkinan penyesuaian:** Nama field `fraction_illumination_percent` menunjukkan satuannya persen. Jika sumber data memakai fraksi 0-1, hasil harus dikonversi.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungLuasCahayaHilal
LuasCahayaHilal(
    fibGhurub = interpolasiAngkaGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        saatGhurub = saatGhurub,
        tabel = TabelEphemeris.BULAN,
        kolom = "fraction_illumination_percent",
        satuan = "persen"
    )
)
```

## Tahap 30: Lebar Nurul Hilal

**Deskripsi:** Menghitung lebar cahaya hilal dalam satuan jari/ushbu.

**Rumus:** Dokumen menulis `NH = (PH^2 + h'^2) / 15`, dengan tanda minus PH dihilangkan.

**Kenapa kode ini dipakai:** Kode menghitung jarak resultan antara posisi horizontal relatif (`PH`) dan tinggi (`h'`) lalu membaginya dengan 15 untuk satuan jari. Ini lebih menyerupai panjang vektor dua dimensi.

**Kemungkinan penyesuaian:** Karena dokumen hasil konversi terbaca tanpa tanda akar, perlu validasi ke dokumen asli/penulis metode apakah akar memang dimaksudkan. Jika dokumen asli benar-benar tidak memakai akar, kode akan menghasilkan nilai lebih kecil dari formula dokumen untuk nilai PH/h' lebih dari 1 derajat.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungLebarNurulHilal
LebarNurulHilal(
    posisiHilalDerajat = posisiHilal.posisiHilalDerajat,
    tinggiBulanMariDerajat = tinggiBulanMariGhurub.tinggiBulanMariDerajat,
    nurulHilalJari = sqrt(
        posisiHilal.posisiHilalDerajat * posisiHilal.posisiHilalDerajat +
            tinggiBulanMariGhurub.tinggiBulanMariDerajat * tinggiBulanMariGhurub.tinggiBulanMariDerajat
    ) / 15.0,
)
```

**Catatan perbedaan:** Implementasi memakai `sqrt(PH^2 + h'^2) / 15`. Ini berbeda dari teks dokumen hasil konversi yang terbaca tanpa akar. Secara geometri, penggunaan akar lebih konsisten untuk jarak resultan.

## Tahap 31: Kemiringan Hilal

**Deskripsi:** Menghitung kemiringan hilal dan menentukan keadaan hilal.

**Rumus:** `MH = atan(PH / h'_bulan)`. Jika `< 15`, hilal terlentang; jika `> 15` dan PH positif, miring ke Utara; jika PH negatif, miring ke Selatan.

**Kenapa kode ini dipakai:** Tahap ini menerjemahkan rasio posisi relatif dan tinggi menjadi label keadaan hilal. Kode memakai nilai absolut untuk besar kemiringan, lalu memakai tanda PH untuk arah miring.

**Kemungkinan penyesuaian:** Jika `h'_bulan` nol atau sangat kecil, pembagian `PH / h'` dapat menjadi sangat besar atau tidak stabil. Perlu uji khusus untuk kasus tinggi mendekati nol.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungKemiringanHilal
val kemiringan = kotlin.math.abs(
    atan(posisiHilal.posisiHilalDerajat / tinggiBulanMariGhurub.tinggiBulanMariDerajat) * 180.0 / PI
)
val keadaan = if (kemiringan < 15.0) {
    "hilal terlentang"
} else if (posisiHilal.posisiHilalDerajat >= 0.0) {
    "hilal miring ke Utara"
} else {
    "hilal miring ke Selatan"
}
```

## Tahap 32: Jarak Busur / Elongasi

**Deskripsi:** Menghitung elongasi geosentrik Bulan-Matahari.

**Rumus:** `JB = arccos(sin(d_matahari) sin(d_bulan) + cos(d_matahari) cos(d_bulan) cos(AR_matahari - AR_bulan))`.

**Kenapa kode ini dipakai:** Elongasi adalah salah satu parameter kriteria awal bulan modern. Kode menghitung jarak sudut geosentrik dari deklinasi dan right ascension Matahari/Bulan pada ghurub.

**Kemungkinan penyesuaian:** Kriteria tertentu bisa memakai elongasi topocentric atau parameter lain. Kode saat ini menghitung elongasi geosentrik sesuai formula dokumen.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungJarakBusurElongasi
val argumenCosinus = sinDeg(deklinasiMatahari) * sinDeg(deklinasiBulan) +
    cosDeg(deklinasiMatahari) * cosDeg(deklinasiBulan) * cosDeg(arMatahari - arBulan)
return JarakBusurElongasi(
    deklinasiMatahariDerajat = deklinasiMatahari,
    deklinasiBulanDerajat = deklinasiBulan,
    asensiorektaMatahariDerajat = arMatahari,
    asensiorektaBulanDerajat = arBulan,
    argumenCosinus = argumenCosinus,
    elongasiDerajat = acos(argumenCosinus.coerceIn(-1.0, 1.0)) * 180.0 / PI,
)
```

## Kesimpulan Awal Bulan

**Deskripsi:** Menentukan prakiraan awal bulan berdasarkan ijtimak sebelum/saat ghurub, tinggi hilal mar'i, dan elongasi sesuai kriteria.

**Algoritma keputusan:** Kode mengevaluasi daftar syarat yang aktif pada `KriteriaAwalBulanFalak`. Untuk kriteria Kemenag/MABIMS terbaru, syarat aktif adalah ijtimak sebelum/saat ghurub, tinggi hilal minimum 3 derajat, dan elongasi minimum 6,4 derajat. Jika semua syarat aktif terpenuhi, awal bulan diprakirakan tanggal setelah tanggal situasi hilal. Jika tidak, kode memakai istikmal dan memprakirakan dua hari setelah tanggal situasi hilal.

**Kemungkinan penyesuaian:** Kode menghasilkan prakiraan berdasarkan kriteria hisab, bukan keputusan sidang isbat. Jika aplikasi harus menampilkan keputusan resmi, hasil ini harus diberi label "prakiraan hisab" sampai ada keputusan resmi.

**Implementasi Kotlin:**

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: susunKesimpulanHisabHilal
val ijtimaSebelumGhurub = saatIjtima.waktuLokal.tanggal.isBefore(saatGhurub.waktuLokal.tanggal) ||
    (saatIjtima.waktuLokal.tanggal == saatGhurub.waktuLokal.tanggal &&
        saatIjtima.waktuLokal.jamDesimal <= saatGhurub.waktuLokal.jamDesimal)
val tinggiMemenuhi = kriteria.tinggiHilalMinimumDerajat?.let {
    tinggiBulanMariGhurub.tinggiBulanMariDerajat >= it
}
val elongasiMemenuhi = kriteria.elongasiMinimumDerajat?.let {
    jarakBusurElongasi.elongasiDerajat >= it
}
val memenuhiKriteria = listOfNotNull(
    if (kriteria.memakaiSyaratIjtimaSebelumGhurub) ijtimaSebelumGhurub else null,
    tinggiMemenuhi,
    elongasiMemenuhi
).all { it }
val tanggalPrakiraan = if (memenuhiKriteria) {
    konteks.tanggalSituasiHilalMasehi.plusDays(1)
} else {
    konteks.tanggalSituasiHilalMasehi.plusDays(2)
}
```

## Gap dan Catatan Audit

- Semua 32 tahap utama pada dokumen awal bulan ditemukan implementasinya dalam `HisabHilalEphemerisCalculator.kt`.
- Tidak ada tahap utama yang tidak ditemukan implementasi Kotlin-nya.
- Perbedaan/penguatan implementasi:
  - Tahap 3, 9, 10, dan 11 memakai normalisasi sudut untuk menghindari kesalahan saat nilai melewati 0/360 derajat.
  - Tahap 4 memakai offset zona waktu dari `ZonaWaktuFalak`, bukan hard-code `+7`.
  - Tahap 30 berbeda dari teks hasil konversi dokumen: implementasi memakai akar kuadrat `sqrt(PH^2 + h'^2) / 15`.
- Rumus bantu visualisasi fisik pada akhir dokumen (`tB`, `BA`, `uh`, `tA`, `u'At`) tidak ditemukan sebagai bagian dari kalkulator 32 tahap. Kode yang ada hanya menyediakan visualisasi ringkas di `VisualHilalMapper.kt`, bukan konstruksi ukuran fisik berbasis jarak 500 cm.

## Matriks Risiko Audit

| Area | Risiko | Dampak | Mitigasi |
|---|---|---:|---|
| Data ephemeris | Kolom tidak tersedia atau nama kolom berubah | Tinggi | Validasi manifest dan skema JSON sebelum kalkulasi |
| Tanggal ephemeris | Data tanggal sebelum/sesudah tidak lengkap | Tinggi | Siapkan buffer tanggal lebih luas, minimal H-1 sampai H+1 untuk kasus batas |
| Longitude/AR 0-360 | Selisih sudut salah saat melewati 0 derajat | Tinggi | Sudah dimitigasi oleh `deltaMajuDerajat`, `selisihSudutBertanda`, dan `ModeInterpolasi.SUDUT_MAJU` |
| Pembulatan manual | Hasil berbeda dari hitungan dokumen karena pembulatan tiap tahap | Sedang | Tentukan standar audit: pembulatan per tahap atau full precision sampai akhir |
| Elevasi markaz | Elevasi GPS/API berbeda dari markaz resmi | Sedang | Gunakan master markaz resmi untuk audit formal |
| Refraksi | Rumus sederhana tidak sama dengan tabel refraksi lokal | Sedang | Bandingkan dengan tabel refraksi yang disyaratkan lembaga |
| Kriteria awal bulan | Kriteria berubah dari MABIMS/Kemenag terbaru | Tinggi | Jadikan `KriteriaAwalBulanFalak` sebagai konfigurasi yang terdokumentasi versinya |
| Tahap 30 | Perbedaan akar kuadrat pada NH | Sedang | Konfirmasi dokumen asli; tambahkan uji numerik untuk dua versi rumus |
| Kesimpulan | Prakiraan hisab disalahartikan sebagai keputusan resmi | Tinggi | Labeli hasil sebagai prakiraan hisab sampai ada keputusan resmi |

## Rekomendasi Pengujian Audit

Untuk menjadikan audit ini kuat secara numerik, disarankan membuat test case dengan format berikut:

1. **Uji satu tanggal dan satu markaz dari contoh manual.**
   - Input: tanggal situasi hilal, lintang, bujur, elevasi, zona waktu.
   - Expected: nilai tiap tahap dari dokumen/manual.
   - Toleransi: misalnya `<= 1 detik waktu` dan `<= 1 detik busur` setelah disepakati.

2. **Uji batas longitude/right ascension 0/360 derajat.**
   - Tujuan: memastikan normalisasi sudut tidak menghasilkan selisih negatif besar.
   - Tahap terdampak: 3, 9, 10, 11, 32.

3. **Uji lokasi dengan zona waktu berbeda.**
   - WIB, WITA, dan WIT.
   - Tujuan: memastikan offset zona dan bujur standar benar.
   - Tahap terdampak: 4, 5, 7, 8, 9-12, 29.

4. **Uji elevasi nol dan elevasi tinggi.**
   - Tujuan: memastikan dip dihitung benar dan tidak menghasilkan nilai tidak wajar.
   - Tahap terdampak: 5, 18, 22.

5. **Uji hilal negatif atau terbenam sebelum ghurub.**
   - Tujuan: memastikan lama hilal negatif ditampilkan dengan makna yang benar.
   - Tahap terdampak: 17, 18, 23, 24, kesimpulan.

6. **Uji kriteria.**
   - Kemenag/MABIMS terbaru: tinggi >= 3 derajat dan elongasi >= 6,4 derajat.
   - Wujudul hilal: tinggi >= 0 derajat dan ijtimak sebelum/saat ghurub.
   - Tanpa kriteria: hanya menampilkan hasil tanpa keputusan visibilitas.

7. **Uji perbandingan dengan data resmi.**
   - Bandingkan hasil ijtimak, ghurub, tinggi hilal, azimut, elongasi, dan FIB dengan tabel resmi Kemenag atau output software astronomi terpercaya.

## Checklist Audit Manual

Gunakan checklist ini saat memeriksa satu hasil hisab:

- [ ] Tanggal situasi hilal adalah tanggal rukyat/29 bulan sebelumnya, bukan tanggal awal bulan target.
- [ ] Markaz memakai lintang, bujur, elevasi, dan zona waktu yang disepakati.
- [ ] Data ephemeris yang dipakai sesuai tahun dan tanggal.
- [ ] FIB terkecil benar-benar dipilih dari tabel Bulan tanggal situasi hilal.
- [ ] ALB dan ELM diambil pada jam FIB dan jam setelahnya.
- [ ] Sabaq Bulan dan Sabaq Matahari dihitung dengan arah gerak maju.
- [ ] Ijtimak lokal dikonversi dari UT dengan offset zona yang benar.
- [ ] Dip memakai elevasi markaz yang benar.
- [ ] KWD memakai bujur standar zona dan bujur markaz.
- [ ] Ghurub menggunakan equation of time dari data Matahari.
- [ ] Interpolasi ghurub memakai jam UT hasil konversi dari waktu lokal.
- [ ] Tinggi hilal mar'i memakai parallax, semi diameter, refraksi, dan dip sesuai tahap.
- [ ] Azimut yang ditampilkan jelas sebagai "dari titik Barat", bukan azimut kompas.
- [ ] Kesimpulan memakai kriteria yang benar dan diberi label sebagai prakiraan hisab.
- [ ] Semua nilai akhir dibandingkan dengan expected value/toleransi audit.

## Rekomendasi Penyempurnaan Kode

Beberapa peningkatan yang disarankan jika sistem akan dipakai untuk audit formal:

- Tambahkan unit test untuk setiap tahap 1-32 dengan fixture data ephemeris kecil.
- Simpan hasil mentah setiap tahap, termasuk `argumenSinus`, `argumenCosinus`, `Na`, `Nb`, `Nc`, dan sumber kolom ephemeris.
- Tampilkan mode interpolasi yang dipakai pada output audit, terutama `LINEAR` versus `SUDUT_MAJU`.
- Tambahkan opsi pembulatan: full precision, pembulatan per tahap, atau pembulatan sesuai format dokumen.
- Tambahkan konfigurasi sumber kriteria dengan versi dan tanggal berlaku, misalnya "MABIMS 2021/2022" atau "Kemenag/MABIMS terbaru".
- Tambahkan validasi bahwa nilai ephemeris Matahari/Bulan untuk jam yang dibutuhkan lengkap sebelum perhitungan dimulai.
- Tambahkan implementasi opsional rumus visualisasi fisik akhir dokumen bila memang diperlukan untuk edukasi kelas/praktikum.
- Tambahkan mode "audit trace" yang mengekspor seluruh `butirPerhitungan` ke Markdown/CSV/JSON untuk lampiran audit.

## Kesimpulan Audit

Secara struktural, implementasi Kotlin sudah mencakup seluruh 32 tahap utama prosedur `EPHEMERIS AWAL BULAN.docx`. Kode tidak hanya menghitung hasil akhir, tetapi juga membentuk `ButirPerhitunganFalak` yang menyimpan nomor tahap, judul, rumus, substitusi, hasil, catatan, dan sumber data ephemeris. Ini merupakan fondasi yang baik untuk audit dan edukasi.

Namun, untuk audit formal tingkat tinggi, masih diperlukan:

- validasi numerik terhadap contoh resmi atau hasil manual;
- keputusan eksplisit tentang pembulatan;
- konfirmasi rumus tahap 30;
- pengujian kasus batas;
- dokumentasi versi kriteria dan versi data ephemeris.

Dengan tambahan pengujian tersebut, dokumen ini dapat dipakai sebagai dasar audit kepatuhan perhitungan Falak awal bulan dan sebagai bahan edukasi teknis tahap demi tahap.

## Audit Acceptance Criteria

Bagian ini mendefinisikan syarat agar implementasi dapat dinilai layak dari sisi audit kepatuhan prosedur.

| Kriteria penerimaan | Status saat ini | Bukti/rujukan |
|---|---|---|
| Semua tahap dokumen utama teridentifikasi | Terpenuhi | Tahap 1-32 dipetakan |
| Semua tahap utama punya implementasi Kotlin | Terpenuhi | Traceability matrix tahap 1-32 |
| Semua dependency utama dicatat | Terpenuhi | Bagian Prasarana, Peta Dependency Fungsi |
| Semua kolom ephemeris yang dipakai terdaftar | Terpenuhi | Bagian Kolom Data Ephemeris |
| Semua normalisasi sudut/waktu dijelaskan | Terpenuhi | Bagian Detail Normalisasi Sudut dan Waktu |
| Perbedaan formula dicatat | Terpenuhi sebagian | Tahap 30 perlu konfirmasi dokumen asli |
| Gap non-utama dicatat | Terpenuhi | Rumus visualisasi fisik akhir dokumen |
| Risiko audit didokumentasikan | Terpenuhi | Matriks Risiko Audit |
| Edge case didokumentasikan | Terpenuhi | Edge Case yang Harus Diuji |
| Validasi numerik terhadap sumber pembanding | Belum termasuk | Akan dilakukan manual oleh auditor |

Kriteria final untuk audit formal:

- Tidak ada tahap utama tanpa fungsi hitung.
- Tidak ada sumber data ephemeris yang tidak dapat ditelusuri.
- Semua perbedaan formula memiliki keputusan: diterima, diperbaiki, atau diberi catatan penggunaan.
- Semua perubahan kriteria awal bulan memiliki versi dan tanggal berlaku.
- Hasil aplikasi tidak boleh diberi label keputusan resmi pemerintah kecuali memang mengambil data keputusan resmi.

## Control Points Audit

Control points adalah titik pemeriksaan yang harus dilalui auditor sebelum menyatakan hasil hisab dapat dipercaya.

| Control point | Bagian yang diperiksa | Pertanyaan audit |
|---|---|---|
| Input markaz | `MarkazFalak`, `buildKonteks`, `FalakMarkazProvider` | Apakah lintang, bujur, elevasi, dan zona waktu benar? |
| Tanggal situasi hilal | `tanggalSituasiHilalMasehi` | Apakah tanggal yang dipakai adalah tanggal rukyat/29 bulan sebelumnya? |
| Data ephemeris | `HisabHilalRepository`, `FalakEphemerisHarian` | Apakah paket data tahun/tanggal yang benar dimuat? |
| Pemilihan FIB | `tentukanDataIjtima` | Apakah FIB terkecil dipilih dari tabel Bulan yang benar? |
| Longitude ijtimak | `tentukanSabaqIjtima`, `hitungSaatIjtima` | Apakah ALB/ELM dan sabaq memakai normalisasi sudut? |
| Konversi waktu | `normalisasiWaktu`, `ZonaWaktuFalak` | Apakah UT ke lokal dan lokal ke UT benar? |
| Ghurub | `hitungSaatGhurub` | Apakah EoT, KWD, dan sudut waktu Matahari benar? |
| Interpolasi | `interpolasiGhurub`, `interpolasiAngkaGhurub` | Apakah Na, Nb, dan Nc berasal dari jam UT yang benar? |
| Koreksi hilal | Tahap 14-18 | Apakah parallax, Sd, refraksi, dan dip diterapkan sesuai prosedur? |
| Evaluasi kriteria | `susunKesimpulanHisabHilal` | Apakah kriteria yang aktif sesuai kebijakan yang disetujui? |

## Failure Mode and Effect Analysis

| Failure mode | Penyebab umum | Dampak | Deteksi | Mitigasi |
|---|---|---|---|---|
| Zona waktu salah | Input zona tidak sesuai bujur | Ijtimak/ghurub bergeser 1-2 jam | Bandingkan zona dengan bujur | Validasi zona otomatis dan peringatan manual |
| Elevasi salah | GPS/API elevasi tidak akurat | Dip dan tinggi hilal mar'i berubah | Bandingkan dengan elevasi markaz resmi | Master data markaz resmi |
| FIB minimum salah | Data Bulan tidak lengkap | Ijtimak salah | Periksa tabel FIB semua jam | Validasi jumlah baris ephemeris |
| ALB/ELM melewati 0 derajat | Tidak normalisasi sudut | Sabaq/ijtimak salah besar | Cek delta longitude | Pakai `deltaMajuDerajat` |
| AR melewati 0 derajat | Interpolasi linear biasa | Sudut waktu/elongasi salah | Cek `ModeInterpolasi` | Pakai `SUDUT_MAJU` |
| EoT salah satuan | Data memakai derajat, kode membaca jam | Ghurub bergeser | Periksa schema data | Validasi tipe `hours` |
| Azimut disalahartikan | Hasil dari Barat dianggap dari Utara | Kesalahan edukasi/arah visual | Periksa label output | Label eksplisit "dari titik Barat" |
| Refraksi tidak sesuai standar | Lembaga memakai tabel refraksi | Tinggi hilal berbeda | Bandingkan tahap 17 | Adapter tabel refraksi |
| Pembulatan terlalu awal | Manual membulatkan setiap tahap | Selisih hasil akhir | Bandingkan metode pembulatan | Tetapkan standar pembulatan |
| Kriteria salah versi | Kebijakan berubah | Kesimpulan awal bulan salah | Audit `KriteriaAwalBulanFalak` | Versi kriteria terdokumentasi |
| Data ephemeris beda versi | Paket cache lama | Semua output bisa berbeda | Periksa manifest/version/hash | Tampilkan versi paket di audit |
| Tahap 30 ambigu | Dokumen terbaca tanpa akar | NH berbeda | Konfirmasi dokumen asli | Decision log tahap 30 |

## Decision Log Teknis

Decision log ini berisi keputusan teknis yang perlu diketahui atau disahkan oleh reviewer.

| Keputusan | Status | Alasan | Dampak |
|---|---|---|---|
| Memakai `Double` full precision sampai akhir | Dipakai | Mengurangi akumulasi error pembulatan | Bisa berbeda dari manual yang membulatkan per tahap |
| Memakai `GMT/UT` sebagai label tabel | Dipakai | Data ephemeris berbasis jam UT | Perlu konsistensi istilah dengan dokumen |
| Memakai `deltaMajuDerajat` untuk selisih longitude | Dipakai | Aman saat melewati 0/360 derajat | Hasil lebih stabil daripada selisih mentah |
| Memakai `ModeInterpolasi.SUDUT_MAJU` untuk AR | Dipakai | AR dapat melewati 0/360 derajat | Mencegah diskontinuitas |
| Memakai clamp `coerceIn(-1, 1)` sebelum `asin/acos` | Dipakai | Menghindari error floating point | Nilai mentah dekat batas tetap perlu diaudit |
| Memakai refraksi rata-rata untuk `ho <= 0` | Dipakai | Sesuai catatan dokumen | Perlu penyesuaian jika lembaga memakai tabel |
| Memakai rumus refraksi untuk `ho > 0` | Dipakai | Sesuai dokumen | Tidak memperhitungkan atmosfer lokal |
| Memakai kriteria Kemenag/MABIMS terbaru sebagai default | Dipakai | Default aplikasi | Harus diberi versi kebijakan |
| Menghasilkan prakiraan, bukan keputusan resmi | Dipakai | Aplikasi hisab, bukan sidang isbat | Label output harus jelas |
| Tahap 30 memakai akar kuadrat | Perlu konfirmasi | Lebih konsisten secara geometri | Bisa berbeda dari teks konversi dokumen |

## Reviewer Guide

Panduan ini membantu reviewer membaca dokumen tanpa harus memahami seluruh kode Android.

Urutan baca yang disarankan:

1. Baca **Status Kelengkapan Audit** untuk mengetahui cakupan.
2. Baca **Ringkasan Algoritma** agar memahami alur besar.
3. Baca **Glosarium Teknis** untuk menyamakan istilah dokumen dan kode.
4. Baca **Traceability Matrix** untuk melihat mapping tahap ke fungsi.
5. Pilih tahap yang ingin diperiksa, lalu baca bagian tahap tersebut.
6. Jika menemukan perbedaan, cek **Analisis Kemungkinan Perbedaan dengan Hitung Manual**.
7. Untuk risiko dan kontrol, baca **Matriks Risiko Audit**, **Control Points**, dan **FMEA**.

Cara menilai satu tahap:

- Pastikan nama tahap cocok dengan dokumen.
- Pastikan rumus di dokumen ditulis ulang dengan benar.
- Pastikan snippet Kotlin menghitung nilai yang sama, bukan hanya menampilkan hasil.
- Periksa apakah ada dependency helper yang mempengaruhi hasil.
- Periksa catatan perbedaan atau kemungkinan penyesuaian.
- Tandai status tahap: sesuai, sesuai dengan catatan, perlu revisi, atau belum dapat diputuskan.

Panduan untuk auditor non-programmer:

- Anggap `fun` sebagai fungsi/prosedur.
- Anggap `val` sebagai nilai yang dihitung sekali.
- Anggap `return` sebagai hasil fungsi.
- Nama seperti `hitungSaatGhurub` berarti fungsi untuk menghitung saat ghurub.
- Nama seperti `tampilkanSaatGhurub` berarti fungsi yang menyiapkan rumus/substitusi/hasil untuk ditampilkan.

## Educational Walkthrough

### Kelompok 1: Persiapan konteks

Tahap 1 menentukan markaz. Ini adalah fondasi karena semua rumus horizon membutuhkan lintang, bujur, elevasi, dan zona waktu. Kesalahan kecil pada markaz dapat mempengaruhi ghurub, tinggi hilal, dan azimut.

### Kelompok 2: Mencari ijtimak

Tahap 2-4 mencari waktu ijtimak dari FIB, ALB, ELM, dan sabaq. Secara edukasi, ini menunjukkan bahwa ijtimak tidak langsung diambil sebagai angka jadi, tetapi dihitung dari pergerakan longitude Matahari dan Bulan pada tabel ephemeris.

### Kelompok 3: Menghitung ghurub

Tahap 5-8 menghitung waktu Matahari terbenam di markaz. Ini penting karena semua posisi hilal dievaluasi pada saat ghurub, bukan sembarang waktu.

### Kelompok 4: Interpolasi data pada ghurub

Tahap 9-12 mengubah data per jam menjadi data pada waktu ghurub. Konsep kuncinya adalah `Na`, `Nb`, dan `Nc`. `Nc` adalah pecahan waktu dari jam atas menuju jam bawah.

### Kelompok 5: Tinggi hilal

Tahap 13-18 menghitung tinggi hilal dari posisi haqiqi sampai mar'i. Di sini koreksi parallax, semi diameter, refraksi, dan dip diperhitungkan.

### Kelompok 6: Lama dan arah hilal

Tahap 19-28 menghitung setengah busur siang, lama hilal, terbenam hilal, azimut, posisi relatif, dan arah terbenam. Bagian ini membantu memahami kondisi hilal di ufuk.

### Kelompok 7: Visibilitas dan kesimpulan

Tahap 29-32 menghitung iluminasi, nurul hilal, kemiringan, dan elongasi. Setelah itu kesimpulan dibandingkan dengan kriteria awal bulan yang aktif.

## Data Provenance

Rantai asal data dalam aplikasi:

1. **Sumber resmi/paket data Falak**
   Data ephemeris tersedia sebagai paket data Kemenag atau sumber resmi yang telah dikonversi ke JSON.

2. **Cache lokal aplikasi**
   `FalakRepositoryImpl` membaca data lokal atau melakukan refresh paket bila data belum tersedia.

3. **Model data**
   `FalakModels.kt` mendefinisikan `FalakDataLengkap`, `FalakEphemerisHarian`, dan `FalakHourlyTable`.

4. **Repository hisab**
   `HisabHilalRepositoryImpl` memilih tanggal yang dibutuhkan dan meneruskan `ephemerisHarian` ke kalkulator.

5. **Kalkulator domain**
   `HisabHilalEphemerisCalculator` menghitung tahap 1-32.

6. **Butir audit**
   `ButirPerhitunganFalak` menyimpan nomor, rumus, substitusi, hasil, catatan, dan sumber ephemeris.

7. **UI**
   `HisabHilalScreen` menampilkan hasil, kesimpulan, dan rincian perhitungan.

Rantai ini penting karena audit tidak cukup hanya melihat hasil akhir. Auditor harus bisa menelusuri hasil akhir kembali ke paket data, tanggal, jam UT, tabel, dan kolom yang dipakai.

## Workflow Fitur Hisab Hilal End-to-End

Bagian ini menjelaskan bagaimana fitur bekerja sebagai satu sistem, mulai dari dependency injection, layar UI, ViewModel, repository, kalkulator domain, sampai hasil ditampilkan kembali. Ini penting untuk audit fitur karena kesalahan tidak selalu berada pada rumus; bisa juga muncul pada input UI, cache data, state ViewModel, atau loading repository.

Catatan penting: workflow ini memiliki dua rantai besar yang harus dipisahkan dalam audit:

- **Workflow data:** data ephemeris tahunan Kemenag dikonversi menjadi paket JSON, dipublikasikan melalui Supabase, diunduh ke cache aplikasi, lalu dibaca repository.
- **Workflow user:** pengguna membuka fitur, memilih parameter, menjalankan hisab, lalu membaca hasil dan rincian tahap.

### Ringkasan layer

| Layer | File utama | Tanggung jawab | Tidak boleh bertanggung jawab atas |
|---|---|---|---|
| Dependency Injection | `di/AppModule.kt` | Mendaftarkan repository, calculator, provider markaz | Menghitung rumus |
| UI Compose | `ui/falak/HisabHilalScreen.kt` | Menampilkan form, tombol, loading, error, hasil, rincian tahap | Mengubah rumus atau data ephemeris |
| ViewModel | `ui/falak/HisabHilalViewModel.kt` | Menyimpan state, validasi input dasar, membangun konteks, memanggil repository | Menghitung 32 tahap |
| Repository Hisab | `data/repository/HisabHilalRepository.kt` | Menyiapkan ephemeris tanggal terkait, memanggil calculator | Menentukan rumus astronomi |
| Repository Falak | `data/repository/FalakRepositoryImpl.kt` | Memuat/refresh paket data, validasi hash, membaca JSON | Menginterpretasi kriteria awal bulan |
| Domain Calculator | `domain/falak/HisabHilalEphemerisCalculator.kt` | Menghitung 32 tahap dan kesimpulan | Mengelola UI/cache/network |
| Domain Models | `domain/falak/HisabHilalModels.kt` | Struktur input, output, dan butir audit | Mengambil data remote |
| Data Models | `data/model/falak/FalakModels.kt` | Struktur paket, manifest, ephemeris JSON | Menghitung hisab |

### Sequence utama saat pengguna menekan tombol Hitung

1. `HisabHilalScreen` menampilkan `HisabInputCard`.
2. Pengguna memilih bulan Hijriah, tanggal, markaz, dan kriteria.
3. Perubahan input memanggil fungsi ViewModel seperti `ubahBulanHijriah`, `ubahTanggalSituasi`, `ubahMarkaz`, atau `ubahKriteria`.
4. `HisabHilalViewModel` menyimpan perubahan dalam `HisabHilalUiState`.
5. Pengguna menekan tombol `Hitung Awal Bulan`.
6. UI memanggil `viewModel.hitung()`.
7. ViewModel menjalankan `buildKonteks(...)`.
8. `buildKonteks(...)` mengubah input string menjadi `KonteksHisabHilal`.
9. ViewModel mengubah state menjadi `loading = true`.
10. ViewModel memanggil `hisabHilalRepository.hitung(konteks)`.
11. `HisabHilalRepositoryImpl.hitung(...)` memanggil `persiapkanEphemeris(konteks)`.
12. Repository menentukan tanggal ephemeris yang diperlukan: tanggal situasi hilal dan tanggal setelahnya.
13. Repository memuat paket data dari `FalakRepository`.
14. Jika cache lokal belum tersedia, `FalakRepositoryImpl` mencoba `refreshPaketKemenag(tahun)`.
15. `FalakRepositoryImpl` mengunduh/validasi manifest dan file JSON berdasarkan checksum.
16. Repository hisab memfilter `ephemerisHarian` sesuai tanggal yang diperlukan.
17. Repository hisab memastikan semua tanggal tersedia.
18. Repository hisab memanggil `HisabHilalEphemerisCalculator.hitung(konteks, ephemerisHarian)`.
19. Calculator menjalankan tahap 1-32 dan menyusun `HasilHisabHilalEphemeris`.
20. Calculator juga menyusun `butirPerhitungan` untuk audit rumus/substitusi/sumber.
21. Repository mengembalikan `Result.success(hasil)`.
22. ViewModel mengubah state menjadi `loading = false`, `hasil = hasil`, `error = null`.
23. `HisabHilalScreen` otomatis recomposition karena `uiState` berubah.
24. UI menampilkan visual hilal, kesimpulan, evaluasi kriteria, ringkasan data, dan accordion rincian tahap.

### Workflow data Kemenag ke JSON Supabase

Bagian ini menjelaskan provenance data sebelum kalkulator menerima `ephemerisHarian`.

1. **Rilis data ephemeris tahunan Kemenag.**
   Data ephemeris tahunan menjadi sumber otoritatif awal. Pada project ini, data tersebut tidak langsung dibaca sebagai PDF/dokumen mentah oleh kalkulator, tetapi sudah dikonversi menjadi paket data terstruktur.

2. **Konversi data mentah ke JSON.**
   Data hasil konversi disusun menjadi beberapa file JSON, antara lain:
   - `manifest.json`;
   - `ephemeris-harian.json`;
   - `hilal-lokasi.json`;
   - `indeks-pencarian.json`;
   - `halaman-pdf.json.gz`.

3. **Manifest menyimpan metadata paket.**
   `manifest.json` memuat kode paket, judul, tahun, versi, jenis sumber, sumber resmi, zona waktu data, rentang tanggal, jumlah record, daftar berkas, ukuran file, dan SHA-256 tiap berkas.

4. **Metadata paket tersedia di tabel Supabase.**
   Aplikasi mengambil paket aktif dari tabel `falak_paket_data` melalui PostgREST dengan filter:
   - `status = aktif`;
   - `jenis_sumber = kemenag`;
   - `tahun = tahun yang dibutuhkan`.

5. **Berkas paket tersedia di Supabase Storage publik.**
   `FalakRepositoryImpl.publicObjectUrl(...)` membentuk URL:
   `SUPABASE_URL/storage/v1/object/public/falak-ephemeris/{path}`.

6. **Aplikasi mengunduh manifest.**
   Jika manifest lokal belum ada atau hash manifest tidak cocok dengan metadata paket, aplikasi mengunduh ulang manifest.

7. **Aplikasi mengunduh semua berkas manifest.**
   Untuk setiap berkas pada `manifest.berkas`, aplikasi memeriksa:
   - file lokal ada atau tidak;
   - ukuran file sama dengan `ukuranBytes`;
   - SHA-256 lokal sama dengan `sha256` manifest.

8. **Validasi integritas setelah unduh.**
   Setelah file diunduh, aplikasi kembali memeriksa ukuran dan SHA-256. Jika gagal, proses berhenti dengan error `Checksum gagal`.

9. **Aplikasi membaca paket JSON.**
   `readDataLengkap(...)` membaca:
   - `ephemeris-harian.json` menjadi `FalakEphemerisHarianFile`;
   - `hilal-lokasi.json` menjadi `FalakHilalLokasiFile`;
   - `indeks-pencarian.json` menjadi `FalakIndeksFile`.

10. **Repository hisab memilih tanggal yang diperlukan.**
    `HisabHilalRepositoryImpl` memfilter `ephemerisHarian` hanya untuk tanggal situasi hilal dan tanggal setelahnya.

11. **Kalkulator menerima data final.**
    `HisabHilalEphemerisCalculator.hitung(...)` hanya menerima `List<FalakEphemerisHarian>` yang sudah dipilih oleh repository.

Workflow data dalam bentuk teks:

```text
Data Ephemeris Tahunan Kemenag
  -> proses konversi internal menjadi JSON terstruktur
    -> manifest.json + ephemeris-harian.json + file pendukung
      -> metadata paket di tabel Supabase falak_paket_data
      -> file paket di Supabase Storage bucket falak-ephemeris
        -> FalakRepositoryImpl.fetchPaketAktif(tahun)
        -> FalakRepositoryImpl.downloadToFile(...)
        -> validasi ukuran file dan SHA-256
        -> readDataLengkap(...)
          -> FalakDataLengkap.ephemerisHarian
            -> HisabHilalRepositoryImpl.persiapkanEphemeris(...)
              -> ephemeris tanggal H dan H+1
                -> HisabHilalEphemerisCalculator.hitung(...)
```

Kode terkait:

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/FalakRepositoryImpl.kt
// Fungsi: fetchPaketAktif
private suspend fun fetchPaketAktif(tahun: Int): FalakPaketDataDto? {
    return postgrest.from("falak_paket_data").select {
        filter {
            eq("status", "aktif")
            eq("jenis_sumber", "kemenag")
            eq("tahun", tahun)
        }
        limit(1)
    }.decodeSingleOrNull<FalakPaketDataDto>()
}
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/FalakRepositoryImpl.kt
// Fungsi: publicObjectUrl, refreshPaketKemenag
private fun publicObjectUrl(path: String): String {
    val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    return "$base/storage/v1/object/public/falak-ephemeris/$path"
}

val valid = local.exists() && local.length() == berkas.ukuranBytes && sha256(local) == berkas.sha256
if (!valid) {
    downloadToFile(publicObjectUrl(berkas.pathStorage), local)
    val afterDownloadValid = local.length() == berkas.ukuranBytes && sha256(local) == berkas.sha256
    check(afterDownloadValid) { "Checksum gagal untuk ${berkas.namaBerkas}." }
}
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/FalakRepositoryImpl.kt
// Fungsi: readDataLengkap
private fun readDataLengkap(paket: FalakPaketDataDto, manifest: FalakManifest, dir: File): FalakDataLengkap {
    val ephemeris = json.decodeFromString<FalakEphemerisHarianFile>(
        File(dir, "ephemeris-harian.json").readText()
    ).ephemerisHarian
    val hilal = json.decodeFromString<FalakHilalLokasiFile>(
        File(dir, "hilal-lokasi.json").readText()
    ).hilalLokasi
    val indeks = json.decodeFromString<FalakIndeksFile>(
        File(dir, "indeks-pencarian.json").readText()
    ).indeksPencarian
    return FalakDataLengkap(paket, manifest, ephemeris, hilal, indeks)
}
```

Catatan audit data:

- Dokumen audit ini belum mengaudit proses konversi dari rilis Kemenag ke JSON. Jika ingin audit end-to-end penuh, proses konversi tersebut perlu dokumen sendiri: alat konversi, operator, tanggal proses, checksum input, checksum output, dan validasi hasil ekstraksi.
- Aplikasi sudah memiliki kontrol integritas paket melalui ukuran file dan SHA-256.
- Metadata `sumberResmi`, `versi`, `tahun`, dan `zonaWaktuData` harus ditampilkan atau dilampirkan pada laporan hasil agar auditor tahu paket mana yang dipakai.
- Jika Supabase Storage bersifat publik, kontrol utama berada pada integritas file/hash dan proses publikasi paket.

### Workflow cache aplikasi

Setelah paket tersedia, aplikasi mengelola cache sebagai berikut:

1. `readCacheStatus()` membaca apakah ada paket lokal dengan `manifest.json`.
2. Jika data tahun yang diperlukan ada di memory, `loadDataLengkap(tahun)` memakai memory cache.
3. Jika memory cache tidak ada, repository mencoba membaca file lokal.
4. Jika file lokal tidak tersedia atau tahun tidak cocok, `HisabHilalRepositoryImpl` melakukan fallback ke `refreshPaketKemenag(tahun)`.
5. Setelah refresh berhasil, data disimpan ke:
   - `memoryData`;
   - `memoryDataByYear`;
   - file cache lokal di `filesDir/falak/ephemeris/{kodePaket}`.

Catatan audit cache:

- Cache bisa membuat aplikasi tetap memakai data lama jika metadata tahun sama tetapi paket belum direfresh. Karena itu versi dan hash paket perlu ditampilkan pada hasil audit.
- `loadDataLengkap(tahun)` memilih cache berdasarkan tahun manifest. Jika ada lebih dari satu paket untuk tahun sama, audit perlu memastikan paket aktif yang benar digunakan setelah refresh.

### Workflow user di fitur

Workflow user di dalam fitur berjalan sebagai berikut:

1. **User membuka layar Hisab Hilal.**
   Layar menampilkan form parameter hisab dan state default:
   - bulan Hijriah default;
   - tanggal situasi hilal default;
   - mode tanggal default `ACUAN_KEMENAG`;
   - markaz default;
   - kriteria default Kemenag/MABIMS terbaru.

2. **User memilih acuan Kemenag atau input manual.**
   Jika memakai `ACUAN_KEMENAG`, tanggal situasi hilal diambil dari daftar `acuanKemenag2026`.
   Jika memilih `INPUT_MANUAL`, user dapat mengetik tanggal sendiri.

3. **User mengubah bulan Hijriah.**
   `ubahBulanHijriah(...)` mencari acuan Kemenag yang sesuai. Jika mode masih `ACUAN_KEMENAG`, tanggal situasi hilal ikut diperbarui.

4. **User mengubah tanggal situasi hilal.**
   `ubahTanggalSituasi(...)` otomatis mengubah mode menjadi `INPUT_MANUAL`, membersihkan error, dan menghapus hasil lama.

5. **User menentukan markaz.**
   User dapat:
   - mengisi manual;
   - mendeteksi lokasi GPS;
   - membuka peta eksternal.

6. **Jika user memilih deteksi lokasi.**
   UI meminta permission lokasi. Setelah permission callback berjalan, ViewModel memanggil `deteksiMarkaz()`.

7. **ViewModel memperbarui markaz hasil deteksi.**
   Jika sukses, `markazInput` diisi dari `MarkazFalakTerdeteksi`. Jika gagal, `error` diisi pesan gagal deteksi.

8. **User memilih kriteria.**
   Kriteria yang tersedia berasal dari `KriteriaAwalBulanFalak`, misalnya Kemenag/MABIMS terbaru, wujudul hilal, atau tanpa kriteria.

9. **User menekan tombol Hitung Awal Bulan.**
   UI memanggil `viewModel.hitung()`.

10. **ViewModel membangun konteks.**
    Tanggal, lintang, bujur, elevasi, zona, bulan Hijriah, dan kriteria diubah menjadi `KonteksHisabHilal`.

11. **Sistem menampilkan loading.**
    `loading = true`, sehingga UI menampilkan pesan menyiapkan data ephemeris dan menghitung hisab hilal.

12. **Repository dan calculator bekerja.**
    Data dimuat dari cache/Supabase bila perlu, lalu kalkulator menghitung tahap 1-32.

13. **Jika sukses, hasil tampil.**
    UI menampilkan:
    - visual hilal;
    - kesimpulan;
    - evaluasi kriteria;
    - ringkasan data;
    - rincian perhitungan accordion.

14. **Jika gagal, error tampil.**
    UI menampilkan `AlertCard(message)` dari `state.error`.

15. **Jika user mengubah parameter setelah hasil tampil.**
    ViewModel membersihkan `hasil = null`, sehingga hasil lama tidak tetap tampil untuk parameter baru.

Workflow user dalam bentuk teks:

```text
User membuka HisabHilalScreen
  -> memilih bulan/acuan/tanggal
  -> memilih atau mengisi markaz
  -> memilih kriteria
  -> menekan Hitung Awal Bulan
    -> UI memanggil ViewModel.hitung()
      -> validasi input dan build KonteksHisabHilal
      -> loading true
      -> repository menyiapkan data
      -> calculator menghitung 32 tahap
      -> ViewModel menyimpan hasil
    -> UI menampilkan hasil + rincian audit
```

State transition penting:

| Aksi user/sistem | State yang berubah | Dampak UI |
|---|---|---|
| Ubah bulan | `bulanHijriah`, mungkin `tanggalSituasiHilal`, `hasil = null` | Hasil lama hilang |
| Pilih acuan Kemenag | `bulanHijriah`, `tanggalSituasiHilal`, `modeTanggal`, `hasil = null` | Form mengikuti acuan |
| Ubah tanggal manual | `tanggalSituasiHilal`, `modeTanggal = INPUT_MANUAL`, `hasil = null` | Tanggal bisa diedit |
| Ubah markaz | `markazInput`, `sumberMarkaz`, `hasil = null` | Markaz baru dipakai |
| Ubah kriteria | `kriteria` | Evaluasi berikutnya memakai kriteria baru |
| Deteksi lokasi mulai | `detectingLocation = true` | UI dapat menampilkan status deteksi |
| Deteksi lokasi sukses | `markazInput`, `sumberMarkaz`, `detectingLocation = false` | Markaz terisi otomatis |
| Hitung mulai | `loading = true`, `error = null` | Loading tampil |
| Hitung sukses | `loading = false`, `hasil = hasil`, `error = null` | Hasil tampil |
| Hitung gagal | `loading = false`, `error = pesan` | Alert error tampil |

Catatan audit workflow user:

- Default tanggal/acuan Kemenag di ViewModel adalah data 2026. Jika fitur dipakai tahun lain, daftar acuan harus diperbarui atau mode manual dipakai.
- Tanggal di UI diberi label "Tanggal Rukyat / 29 Bulan Sebelumnya"; ini penting secara edukasi agar user tidak memasukkan tanggal awal bulan target.
- Error input terjadi sebelum repository dipanggil. Error data terjadi setelah repository mencoba load/refresh.
- UI tidak menyimpan hasil permanen; hasil berada pada state runtime ViewModel.

### Sequence dalam bentuk teks

```text
User
  -> HisabHilalScreen
    -> HisabHilalViewModel.hitung()
      -> buildKonteks(HisabHilalUiState)
      -> HisabHilalRepositoryImpl.hitung(konteks)
        -> persiapkanEphemeris(konteks)
          -> FalakRepository.loadDataLengkap(tahun)
          -> jika gagal: FalakRepository.refreshPaketKemenag(tahun)
          -> filter ephemeris tanggal H dan H+1
        -> HisabHilalEphemerisCalculator.hitung(konteks, ephemerisHarian)
          -> tahap 1-32
          -> susunKesimpulanHisabHilal
          -> susun butirPerhitungan
      -> update HisabHilalUiState.hasil
    -> HisabHilalScreen menampilkan hasil dan rincian audit
```

### Dependency injection

Dependency fitur didaftarkan melalui Koin pada `AppModule.kt`:

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/di/AppModule.kt
single<FalakRepository> { FalakRepositoryImpl(androidContext(), get(), get()) }
single { com.alhasanah.alhasanahmedia.domain.falak.HisabHilalEphemerisCalculator() }
single<HisabHilalRepository> { HisabHilalRepositoryImpl(get(), get()) }
single { com.alhasanah.alhasanahmedia.util.FalakMarkazProvider(androidContext(), get()) }
```

Makna audit:

- `HisabHilalRepositoryImpl` selalu menerima `FalakRepository` dan `HisabHilalEphemerisCalculator` dari DI.
- `HisabHilalViewModel` menerima `HisabHilalRepository` dan `FalakMarkazProvider`.
- Jika hasil fitur berubah karena dependency diganti, audit harus memeriksa konfigurasi DI.

### UI Compose

`HisabHilalScreen` adalah titik masuk fitur dari sisi pengguna.

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/ui/falak/HisabHilalScreen.kt
// Fungsi: HisabHilalScreen
val state by viewModel.uiState.collectAsState()
val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    viewModel.deteksiMarkaz()
}
```

UI menghubungkan event pengguna ke ViewModel:

```kotlin
HisabInputCard(
    state = state,
    onBulanHijriahChange = viewModel::ubahBulanHijriah,
    onAcuanChange = viewModel::pilihAcuanKemenag,
    onModeTanggalChange = viewModel::ubahModeTanggal,
    onTanggalChange = viewModel::ubahTanggalSituasi,
    onMarkazChange = viewModel::ubahMarkaz,
    onKriteriaChange = viewModel::ubahKriteria,
    onDetectLocation = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    },
    onOpenMap = { input ->
        openMarkazMap(context = context, input = input)
    },
    onHitung = viewModel::hitung
)
```

UI menampilkan hasil berdasarkan state:

```kotlin
state.error?.let { message ->
    item {
        AlertCard(message = message)
    }
}

if (state.loading) {
    item {
        LoadingCard("Menyiapkan data ephemeris dan menghitung hisab hilal")
    }
}

state.hasil?.let { hasil ->
    item { VisualHilalCard(VisualHilalMapper().map(hasil)) }
    item { KesimpulanCard(hasil) }
    item { EvaluasiKriteriaCard(hasil, state) }
    item { RingkasanDataCard(hasil) }
    item {
        SectionTitle("Rincian Perhitungan", "Buka setiap butir untuk melihat rumus, substitusi, hasil, dan sumber data.")
    }
    items(hasil.butirPerhitungan) { butir ->
        ButirAccordion(butir)
    }
}
```

Catatan audit:

- UI tidak menghitung rumus.
- UI hanya mengumpulkan input dan menampilkan hasil.
- Visualisasi hilal berasal dari `VisualHilalMapper`, bukan bagian kalkulator 32 tahap.

### ViewModel dan state

State utama fitur:

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/ui/falak/HisabHilalViewModel.kt
data class HisabHilalUiState(
    val bulanHijriah: String = "Ramadan 1447 H",
    val tanggalSituasiHilal: String = "2026-02-17",
    val modeTanggal: ModeTanggalHisab = ModeTanggalHisab.ACUAN_KEMENAG,
    val markazInput: MarkazInput = MarkazInput(),
    val kriteria: KriteriaAwalBulanFalak = KriteriaAwalBulanFalak.KemenagMabimsTerbaru,
    val loading: Boolean = false,
    val detectingLocation: Boolean = false,
    val error: String? = null,
    val sumberMarkaz: String? = null,
    val hasil: HasilHisabHilalEphemeris? = null,
)
```

ViewModel membangun konteks:

```kotlin
private fun buildKonteks(state: HisabHilalUiState): KonteksHisabHilal {
    val tanggal = LocalDate.parse(state.tanggalSituasiHilal.trim())
    val input = state.markazInput
    val lintang = input.lintang.toDoubleOrNull() ?: error("Lintang belum valid.")
    val bujur = input.bujur.toDoubleOrNull() ?: error("Bujur belum valid.")
    val elevasi = input.elevasi.toDoubleOrNull() ?: error("Elevasi belum valid.")
    val zona = when (input.zona.trim().uppercase()) {
        "WIB" -> ZonaWaktuFalak.WIB
        "WITA" -> ZonaWaktuFalak.WITA
        "WIT" -> ZonaWaktuFalak.WIT
        else -> FalakMarkazProvider.zonaWaktuIndonesia(bujur)
    }
    return KonteksHisabHilal(
        bulanHijriah = state.bulanHijriah.ifBlank { "Bulan Hijriah" },
        tanggalSituasiHilalMasehi = tanggal,
        markaz = MarkazFalak(
            nama = input.nama.ifBlank { "Markaz" },
            lintangDerajat = lintang,
            bujurDerajat = bujur,
            elevasiMeter = elevasi,
            zonaWaktu = zona,
        ),
        kriteriaAwalBulan = state.kriteria,
    )
}
```

ViewModel menjalankan perhitungan:

```kotlin
fun hitung() {
    viewModelScope.launch {
        val konteks = runCatching { buildKonteks(_uiState.value) }
            .onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Parameter hisab belum valid.") }
            }
            .getOrNull() ?: return@launch

        _uiState.update { it.copy(loading = true, error = null) }
        hisabHilalRepository.hitung(konteks)
            .onSuccess { hasil ->
                _uiState.update { it.copy(loading = false, hasil = hasil, error = null) }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Perhitungan hisab hilal gagal."
                    )
                }
            }
    }
}
```

Catatan audit:

- Validasi input angka terjadi di ViewModel.
- ViewModel belum memvalidasi rentang lintang/bujur secara eksplisit pada jalur manual.
- Error parsing tanggal atau angka akan masuk ke `uiState.error`.
- State `hasil` dihapus saat input penting berubah, sehingga UI tidak menampilkan hasil lama setelah parameter berubah.

### Repository hisab

Repository hisab menyiapkan data sebelum kalkulator dipanggil:

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/HisabHilalRepository.kt
override suspend fun persiapkanEphemeris(konteks: KonteksHisabHilal): Result<DataEphemerisHisabHilal> =
    runCatching {
        val tanggal = konteks.tanggalSituasiHilalMasehi
        val tanggalDiperlukan = setOf(tanggal, tanggal.plusDays(1))
        val paketPerTahun = tanggalDiperlukan
            .map { it.year }
            .distinct()
            .map { tahun ->
                falakRepository.loadDataLengkap(tahun).recoverCatching {
                    falakRepository.refreshPaketKemenag(tahun).getOrThrow()
                }.getOrThrow()
            }
        val ephemeris = paketPerTahun.flatMap { data ->
            data.ephemerisHarian.filter { item ->
                runCatching { LocalDate.parse(item.date) }.getOrNull() in tanggalDiperlukan
            }
        }
        val tersedia = ephemeris.mapTo(mutableSetOf()) { LocalDate.parse(it.date) }
        val hilang = tanggalDiperlukan - tersedia
        check(hilang.isEmpty()) {
            "Data ephemeris tanggal ${hilang.joinToString()} belum tersedia pada paket ${paketPerTahun.joinToString { it.paket.kode }}."
        }
        DataEphemerisHisabHilal(
            paketUtama = paketPerTahun.first(),
            paketPendukung = paketPerTahun.drop(1),
            tanggalSituasiHilalMasehi = tanggal,
            ephemerisHarian = ephemeris.distinctBy { it.date }.sortedBy { it.date },
        )
    }
```

Catatan audit:

- Repository saat ini memuat tanggal H dan H+1.
- Ini cukup untuk banyak kasus, tetapi edge case jam UT negatif atau kebutuhan H-1 perlu dianalisis bila muncul dari tanggal/waktu tertentu.
- Jika data lokal tidak tersedia, repository mencoba refresh paket Kemenag.

### Repository data Falak dan cache

`FalakRepositoryImpl` bertanggung jawab memuat data paket:

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/FalakRepositoryImpl.kt
override suspend fun refreshPaketKemenag(tahun: Int): Result<FalakDataLengkap> = withContext(Dispatchers.IO) {
    runCatching {
        val paket = fetchPaketAktif(tahun) ?: error("Paket Falak Kemenag $tahun belum aktif.")
        val dir = File(cacheRoot, paket.kode).apply { mkdirs() }
        val manifestPath = File(dir, "manifest.json")
        val needsManifest = !manifestPath.exists() ||
            paket.sha256Manifest?.let { sha256(manifestPath) != it } == true

        if (needsManifest) {
            downloadToFile(publicObjectUrl(paket.pathManifestStorage ?: error("Path manifest kosong.")), manifestPath)
        }

        val manifest = json.decodeFromString<FalakManifest>(manifestPath.readText())
        for (berkas in manifest.berkas) {
            val local = File(dir, berkas.namaBerkas)
            val valid = local.exists() && local.length() == berkas.ukuranBytes && sha256(local) == berkas.sha256
            if (!valid) {
                downloadToFile(publicObjectUrl(berkas.pathStorage), local)
                val afterDownloadValid = local.length() == berkas.ukuranBytes && sha256(local) == berkas.sha256
                check(afterDownloadValid) { "Checksum gagal untuk ${berkas.namaBerkas}." }
            }
        }
        val data = readDataLengkap(paket, manifest, dir)
        memoryData = data
        memoryDataByYear[paket.tahun] = data
        statusFlow.value = FalakCacheStatus(true, paket, manifest, dir.absolutePath)
        data
    }
}
```

Membaca data lokal:

```kotlin
private fun readDataLengkap(paket: FalakPaketDataDto, manifest: FalakManifest, dir: File): FalakDataLengkap {
    val ephemeris = json.decodeFromString<FalakEphemerisHarianFile>(
        File(dir, "ephemeris-harian.json").readText()
    ).ephemerisHarian
    val hilal = json.decodeFromString<FalakHilalLokasiFile>(
        File(dir, "hilal-lokasi.json").readText()
    ).hilalLokasi
    val indeks = json.decodeFromString<FalakIndeksFile>(
        File(dir, "indeks-pencarian.json").readText()
    ).indeksPencarian
    return FalakDataLengkap(paket, manifest, ephemeris, hilal, indeks)
}
```

Catatan audit:

- Ada validasi `sha256` dan ukuran file untuk berkas paket.
- Data yang dipakai kalkulator berasal dari `ephemeris-harian.json`.
- Manifest menyimpan metadata tahun, versi, sumber resmi, dan rentang tanggal.
- Audit profesional sebaiknya mencatat `manifest.kode`, `manifest.versi`, `manifest.sumberResmi`, dan hash file pada laporan hasil.

### Domain calculator

Domain calculator bersifat murni relatif terhadap input: menerima `KonteksHisabHilal` dan `List<FalakEphemerisHarian>`, lalu menghasilkan `HasilHisabHilalEphemeris`.

Karakter domain:

- Tidak membaca UI.
- Tidak mengakses network.
- Tidak mengakses cache/file.
- Tidak membuat keputusan tampilan.
- Fokus pada perhitungan tahap 1-32 dan kesimpulan.

Implikasi audit:

- Jika input konteks dan ephemeris sama, hasil kalkulator seharusnya deterministik.
- Ini layer terbaik untuk unit test.
- Jika ada selisih angka, audit pertama harus memeriksa input konteks dan data ephemeris sebelum menyalahkan rumus.

### Output dan rendering hasil

Output utama adalah `HasilHisabHilalEphemeris`, yang memuat:

- `konteks`;
- hasil setiap tahap sebagai properti typed;
- `kesimpulan`;
- `butirPerhitungan`.

UI memakai output ini untuk:

- visual hilal;
- kartu kesimpulan;
- evaluasi kriteria;
- ringkasan data;
- accordion rincian tahap.

Bagian audit terpenting adalah `butirPerhitungan`, karena berisi:

- nomor tahap;
- rumus;
- substitusi;
- hasil;
- catatan;
- sumber ephemeris.

### Error handling dan titik gagal

| Titik gagal | Lokasi | Efek UI | Catatan audit |
|---|---|---|---|
| Tanggal input tidak valid | `buildKonteks` | `error = Parameter hisab belum valid` | Perlu pesan lebih spesifik jika dibutuhkan |
| Lintang/bujur/elevasi bukan angka | `buildKonteks` | Error validasi | Sudah ada pesan per field |
| Izin lokasi tidak diberikan | `FalakMarkazProvider.deteksiMarkaz` | Error deteksi markaz | Tidak menghalangi input manual |
| Cache Falak belum tersedia | `FalakRepositoryImpl.readLocalDataLengkap` | Repository mencoba refresh | Butuh network jika cache kosong |
| Paket tahun belum aktif | `fetchPaketAktif` | Error perhitungan | Harus ditampilkan jelas ke user |
| Checksum gagal | `refreshPaketKemenag` | Error perhitungan/cache | Kontrol integritas berjalan |
| Data tanggal hilang | `HisabHilalRepositoryImpl.persiapkanEphemeris` | Error perhitungan | Pesan menyebut tanggal hilang |
| Kolom ephemeris hilang | `nilaiDerajat/nilaiJam/nilaiAngka` | Error perhitungan | Pesan menyebut kolom dan jam |
| Nilai trig tidak valid | `coerceIn` mencegah crash | Tidak selalu error | Nilai mentah perlu diaudit jika dekat batas |

### Batas tanggung jawab tiap layer

| Pertanyaan | Layer yang harus menjawab |
|---|---|
| Bulan/tanggal/markaz apa yang dipakai? | ViewModel/UI state |
| Data ephemeris tahun apa yang dipakai? | Repository Hisab dan FalakRepository |
| File JSON apa yang dibaca? | FalakRepositoryImpl |
| Kolom ephemeris mana yang dipakai? | Calculator helper `nilai...` |
| Rumus tahap 1-32 apa yang dipakai? | Domain calculator |
| Mengapa hasil memenuhi/tidak memenuhi kriteria? | `susunKesimpulanHisabHilal` |
| Bagaimana hasil ditampilkan ke pengguna? | HisabHilalScreen |
| Apakah data resmi dan tidak korup? | FalakRepositoryImpl, manifest, sha256 |

### Rekomendasi peningkatan workflow

- Tampilkan metadata paket data pada UI hasil: kode paket, tahun, versi, sumber resmi, status cache.
- Simpan `DataEphemerisHisabHilal` atau metadata paket di `HasilHisabHilalEphemeris` agar audit hasil tidak perlu menelusuri repository.
- Tambahkan validasi rentang lintang/bujur pada `buildKonteks`.
- Tambahkan buffer tanggal H-1 bila ditemukan kasus UT negatif pada interpolasi.
- Tambahkan export hasil `butirPerhitungan` ke Markdown/CSV/JSON.
- Tambahkan event log ringkas: input diterima, data dimuat, kalkulator selesai, hasil ditampilkan.
- Pisahkan visualisasi hilal dari audit kalkulator dengan label edukatif yang jelas.

## Change Impact Analysis

| Jenis perubahan | File terdampak | Tahap terdampak | Catatan |
|---|---|---|---|
| Format JSON ephemeris berubah | `FalakModels.kt`, helper `nilaiDerajat/nilaiJam/nilaiAngka` | Semua tahap berbasis ephemeris | Perlu migrasi schema |
| Nama kolom ephemeris berubah | `HisabHilalEphemerisCalculator.kt` | 2, 5, 6, 8-10, 12, 14-15, 29 | Risiko error runtime |
| Kriteria MABIMS berubah | `HisabHilalModels.kt`, `susunKesimpulanHisabHilal` | Kesimpulan | Perlu versi kriteria |
| Refraksi diganti tabel | `hitungRefraksiHilal` | 17, 18, 22 | Tambahkan sumber tabel |
| Ghurub perlu EoT interpolasi | `hitungSaatGhurub` | 8 dan turunannya | Hasil 9-32 ikut berubah |
| Markaz harus resmi | `FalakMarkazProvider`, `HisabHilalViewModel` | 1 dan banyak tahap | Batasi input manual |
| Output audit perlu ekspor | UI/repository tambahan | Semua tahap | Dapat ekspor `butirPerhitungan` |
| Tahap 30 dikoreksi | `hitungLebarNurulHilal`, `tampilkanLebarNurulHilal` | 30, kesimpulan ringkasan | Perlu keputusan formula |
| Visualisasi fisik akhir dokumen ditambahkan | `VisualHilalMapper` atau kalkulator baru | Lampiran visualisasi | Jangan campur dengan tahap 1-32 |

## Audit Checklist by Role

### Developer

- [ ] Semua fungsi tahap 1-32 tetap ada setelah perubahan kode.
- [ ] Semua kolom ephemeris yang dipakai masih sesuai schema.
- [ ] Unit test edge case sudut 0/360 tersedia.
- [ ] `ButirPerhitunganFalak` tetap mengandung rumus, substitusi, hasil, dan sumber.
- [ ] Tidak ada perubahan kriteria tanpa update dokumentasi.

### Ahli Falak

- [ ] Rumus tiap tahap sesuai metode yang disepakati.
- [ ] Konvensi azimut dari titik Barat sudah benar.
- [ ] Refraksi dan dip sesuai standar lembaga.
- [ ] Tahap 30 diputuskan: memakai akar atau tidak.
- [ ] Kriteria awal bulan sesuai kebijakan terbaru.

### QA / Tester

- [ ] Uji WIB, WITA, dan WIT.
- [ ] Uji hilal positif dan negatif.
- [ ] Uji tanggal yang melewati akhir tahun.
- [ ] Uji data ephemeris tidak lengkap.
- [ ] Uji nilai dekat ambang tinggi dan elongasi.

### Auditor Kepatuhan

- [ ] Ada daftar file relevan dan file diabaikan.
- [ ] Ada traceability tahap ke fungsi.
- [ ] Ada daftar dependency dan helper.
- [ ] Ada daftar risiko dan mitigasi.
- [ ] Ada keputusan tertulis untuk setiap perbedaan formula.

### Pengajar

- [ ] Materi dapat dibagi menjadi kelompok ijtimak, ghurub, posisi hilal, dan kesimpulan.
- [ ] Istilah teknis tersedia di glosarium.
- [ ] Siswa dapat mengikuti rumus, substitusi, dan hasil.
- [ ] Perbedaan hitung manual dan kode dijelaskan.
- [ ] Edge case dijadikan bahan diskusi.

## Known Limitations

- Dokumen ini tidak menggantikan validasi numerik manual.
- Dokumen ini tidak menyatakan hasil aplikasi sebagai keputusan resmi awal bulan.
- Kode tidak memperhitungkan cuaca, kualitas atmosfer aktual, atau kemampuan observasi manusia.
- Refraksi disederhanakan sesuai rumus/nilai rata-rata dalam dokumen.
- Elevasi otomatis dapat berbeda dari elevasi markaz resmi.
- Data ephemeris bergantung pada versi paket data yang tersedia.
- Visualisasi hilal di aplikasi bersifat edukatif, bukan bukti observasi astronomis.
- Rumus visualisasi fisik akhir dokumen belum diimplementasikan sebagai kalkulator utama.
- Tahap 30 masih perlu konfirmasi formula asli.
- Audit ini belum mencakup keamanan distribusi paket data, hash, atau validasi integritas end-to-end secara penuh.

## Lampiran Source Excerpt Lengkap

Lampiran ini berisi excerpt kode yang paling penting untuk audit. Tujuannya agar reviewer dapat melihat alur asli implementasi tanpa harus membuka semua file Kotlin. Excerpt ini tidak menggantikan file sumber; untuk audit final, file sumber tetap menjadi rujukan utama.

### A. Orkestrasi 32 tahap

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitung
fun hitung(
    konteks: KonteksHisabHilal,
    ephemerisHarian: List<FalakEphemerisHarian>,
): HasilHisabHilalEphemeris {
    val ephemerisPerTanggal = ephemerisHarian.associateBy { LocalDate.parse(it.date) }
    val dataIjtima = tentukanDataIjtima(konteks, ephemerisPerTanggal)
    val sabaqIjtima = tentukanSabaqIjtima(dataIjtima)
    val saatIjtima = hitungSaatIjtima(konteks, dataIjtima, sabaqIjtima)
    val posisiMatahariHaqiqiGhurub = hitungPosisiMatahariHaqiqiGhurub(konteks, ephemerisPerTanggal)
    val sudutWaktuMatahariGhurub = hitungSudutWaktuMatahariGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        posisiMatahariHaqiqiGhurub = posisiMatahariHaqiqiGhurub
    )
    val koreksiWaktuDaerah = hitungKoreksiWaktuDaerah(konteks)
    val saatGhurub = hitungSaatGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        sudutWaktuMatahariGhurub = sudutWaktuMatahariGhurub,
        koreksiWaktuDaerah = koreksiWaktuDaerah
    )
    val asensiorektaMatahariGhurub = hitungAsensiorektaMatahariGhurub(konteks, ephemerisPerTanggal, saatGhurub)
    val asensiorektaBulanGhurub = hitungAsensiorektaBulanGhurub(konteks, ephemerisPerTanggal, saatGhurub)
    val sudutWaktuBulanGhurub = hitungSudutWaktuBulanGhurub(
        sudutWaktuMatahariGhurub = sudutWaktuMatahariGhurub,
        asensiorektaMatahariGhurub = asensiorektaMatahariGhurub,
        asensiorektaBulanGhurub = asensiorektaBulanGhurub
    )
    val deklinasiGhurub = hitungDeklinasiGhurub(konteks, ephemerisPerTanggal, saatGhurub)
    val tinggiBulanHaqiqiGhurub = hitungTinggiBulanHaqiqiGhurub(
        konteks = konteks,
        sudutWaktuBulanGhurub = sudutWaktuBulanGhurub,
        deklinasiGhurub = deklinasiGhurub
    )
    val parallaxBulanGhurub = hitungParallaxBulanGhurub(
        konteks = konteks,
        ephemerisPerTanggal = ephemerisPerTanggal,
        saatGhurub = saatGhurub,
        tinggiBulanHaqiqiGhurub = tinggiBulanHaqiqiGhurub
    )
    val semiDiameterBulanGhurub = hitungSemiDiameterBulanGhurub(konteks, ephemerisPerTanggal, saatGhurub)
    val hoBulanGhurub = hitungHoBulanGhurub(
        tinggiBulanHaqiqiGhurub = tinggiBulanHaqiqiGhurub,
        parallaxBulanGhurub = parallaxBulanGhurub,
        semiDiameterBulanGhurub = semiDiameterBulanGhurub
    )
    val refraksiHilal = hitungRefraksiHilal(hoBulanGhurub)
    val tinggiBulanMariGhurub = hitungTinggiBulanMariGhurub(
        posisiMatahariHaqiqiGhurub = posisiMatahariHaqiqiGhurub,
        hoBulanGhurub = hoBulanGhurub,
        refraksiHilal = refraksiHilal
    )
    val nishfulFadhlahBulan = hitungNishfulFadhlahBulan(konteks, deklinasiGhurub)
    val parallaxNishfulFadhlah = hitungParallaxNishfulFadhlah(
        nishfulFadhlahBulan = nishfulFadhlahBulan,
        parallaxBulanGhurub = parallaxBulanGhurub
    )
    val setengahBusurSiangBulanHaqiqi = hitungSetengahBusurSiangBulanHaqiqi(nishfulFadhlahBulan)
    val setengahBusurSiangBulan = hitungSetengahBusurSiangBulan(
        posisiMatahariHaqiqiGhurub = posisiMatahariHaqiqiGhurub,
        semiDiameterBulanGhurub = semiDiameterBulanGhurub,
        nishfulFadhlahBulan = nishfulFadhlahBulan,
        parallaxNishfulFadhlah = parallaxNishfulFadhlah,
        setengahBusurSiangBulanHaqiqi = setengahBusurSiangBulanHaqiqi
    )
    val lamaHilalMukuts = hitungLamaHilalMukuts(
        setengahBusurSiangBulan = setengahBusurSiangBulan,
        sudutWaktuBulanGhurub = sudutWaktuBulanGhurub
    )
    val terbenamHilal = hitungTerbenamHilal(saatGhurub, lamaHilalMukuts)
    val azimutMatahariGhurub = hitungAzimutMatahariGhurub(konteks, sudutWaktuMatahariGhurub)
    val azimutBulanGhurub = hitungAzimutBulanGhurub(konteks, sudutWaktuBulanGhurub, deklinasiGhurub)
    val posisiHilal = hitungPosisiHilal(azimutMatahariGhurub, azimutBulanGhurub)
    val arahTerbenamHilal = hitungArahTerbenamHilal(konteks, setengahBusurSiangBulan, deklinasiGhurub)
    val luasCahayaHilal = hitungLuasCahayaHilal(konteks, ephemerisPerTanggal, saatGhurub)
    val lebarNurulHilal = hitungLebarNurulHilal(posisiHilal, tinggiBulanMariGhurub)
    val kemiringanHilal = hitungKemiringanHilal(posisiHilal, tinggiBulanMariGhurub)
    val jarakBusurElongasi = hitungJarakBusurElongasi(
        asensiorektaMatahariGhurub = asensiorektaMatahariGhurub,
        asensiorektaBulanGhurub = asensiorektaBulanGhurub,
        deklinasiGhurub = deklinasiGhurub
    )
```

Catatan audit: excerpt ini menunjukkan urutan aktual tahap 1-32. Kode lanjutan pada fungsi yang sama membentuk `HasilHisabHilalEphemeris` dan `butirPerhitungan` bernomor 1 sampai 32.

### B. Data ijtimak dan ghurub

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: tentukanDataIjtima, tentukanSabaqIjtima, hitungSaatIjtima
private fun tentukanDataIjtima(
    konteks: KonteksHisabHilal,
    ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
): DataIjtima {
    val tanggal = konteks.tanggalSituasiHilalMasehi
    val dataTanggal = ephemerisPerTanggal[tanggal] ?: error("Data ephemeris tanggal $tanggal tidak tersedia.")
    val barisFibTerkecil = dataTanggal.hourlyTable.moon.minByOrNull { row ->
        row.doubleAt("fraction_illumination_percent") ?: Double.POSITIVE_INFINITY
    } ?: error("Data Bulan tanggal $tanggal kosong.")
    val jamFibUt = barisFibTerkecil.hourUt()
    val bulanJamSetelahnya = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt + 1, TabelEphemeris.BULAN)
    val matahariJamFib = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt, TabelEphemeris.MATAHARI)
    val matahariJamSetelahnya = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt + 1, TabelEphemeris.MATAHARI)

    return DataIjtima(
        fibTerkecilPersen = nilaiAngka(tanggal, jamFibUt, TabelEphemeris.BULAN, "fraction_illumination_percent", barisFibTerkecil),
        jamFibUt = jamFibUt,
        albJamFib = nilaiDerajat(tanggal, jamFibUt, TabelEphemeris.BULAN, "apparent_longitude", barisFibTerkecil),
        albJamSetelahnya = nilaiDerajat(bulanJamSetelahnya.tanggal, bulanJamSetelahnya.jamUt, TabelEphemeris.BULAN, "apparent_longitude", bulanJamSetelahnya.row),
        elmJamFib = nilaiDerajat(tanggal, jamFibUt, TabelEphemeris.MATAHARI, "apparent_ecliptic_longitude", matahariJamFib.row),
        elmJamSetelahnya = nilaiDerajat(matahariJamSetelahnya.tanggal, matahariJamSetelahnya.jamUt, TabelEphemeris.MATAHARI, "apparent_ecliptic_longitude", matahariJamSetelahnya.row),
    )
}

private fun tentukanSabaqIjtima(dataIjtima: DataIjtima): SabaqIjtima =
    SabaqIjtima(
        sabaqBulanDerajat = deltaMajuDerajat(dataIjtima.albJamFib.nilai, dataIjtima.albJamSetelahnya.nilai),
        sabaqMatahariDerajat = deltaMajuDerajat(dataIjtima.elmJamFib.nilai, dataIjtima.elmJamSetelahnya.nilai),
    )

private fun hitungSaatIjtima(
    konteks: KonteksHisabHilal,
    dataIjtima: DataIjtima,
    sabaqIjtima: SabaqIjtima,
): SaatIjtima {
    val jarakElmAlb = selisihSudutBertanda(dataIjtima.elmJamFib.nilai, dataIjtima.albJamFib.nilai)
    val jamIjtimaUt = dataIjtima.jamFibUt + (
        jarakElmAlb / (sabaqIjtima.sabaqBulanDerajat - sabaqIjtima.sabaqMatahariDerajat)
        )
    return SaatIjtima(
        jarakElmAlbDerajat = jarakElmAlb,
        waktuUt = normalisasiWaktu(konteks.tanggalSituasiHilalMasehi, jamIjtimaUt, "GMT/UT"),
        waktuLokal = normalisasiWaktu(
            konteks.tanggalSituasiHilalMasehi,
            jamIjtimaUt + konteks.markaz.zonaWaktu.offsetJam,
            konteks.markaz.zonaWaktu.nama
        ),
    )
}
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungPosisiMatahariHaqiqiGhurub, hitungSudutWaktuMatahariGhurub, hitungKoreksiWaktuDaerah, hitungSaatGhurub
val refraksiGhurub = 34.5 / 60.0
val dip = sqrt(konteks.markaz.elevasiMeter.coerceAtLeast(0.0)) * 0.0293

val argumenCosinus = -tanDeg(lintang) * tanDeg(deklinasi) +
    (sinDeg(tinggi) / cosDeg(lintang) / cosDeg(deklinasi))

private fun hitungKoreksiWaktuDaerah(konteks: KonteksHisabHilal): KoreksiWaktuDaerah =
    KoreksiWaktuDaerah(
        bujurStandarDerajat = konteks.markaz.zonaWaktu.bujurStandarDerajat,
        bujurMarkazDerajat = konteks.markaz.bujurDerajat,
        koreksiJam = (konteks.markaz.zonaWaktu.bujurStandarDerajat - konteks.markaz.bujurDerajat) / 15.0,
    )

val jamGhurub = (sudutWaktuMatahariGhurub.sudutWaktuDerajat / 15.0) +
    (12.0 - equationOfTime.nilai) +
    koreksiWaktuDaerah.koreksiJam
```

Catatan audit: blok kedua adalah excerpt ringkas dari beberapa fungsi ghurub. Fungsi lengkapnya ada pada file sumber, sedangkan bagian yang ditampilkan di sini adalah baris formula inti.

### C. Tinggi hilal, koreksi, azimut, dan elongasi

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungTinggiBulanHaqiqiGhurub, hitungParallaxBulanGhurub, hitungHoBulanGhurub, hitungRefraksiHilal
val argumenSinus = sinDeg(lintang) * sinDeg(deklinasiBulan) +
    cosDeg(lintang) * cosDeg(deklinasiBulan) * cosDeg(sudutWaktuBulan)

parallaxDerajat = hp.hasilDerajat * cosDeg(tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat)

hoDerajat = tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat -
    parallaxBulanGhurub.parallaxDerajat +
    semiDiameterBulanGhurub.interpolasi.hasilDerajat

val refraksi = if (ho <= 0.0) {
    34.5 / 60.0
} else {
    0.0167 / tanDeg(ho + 7.31 / (ho + 4.4))
}
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: hitungSetengahBusurSiangBulan, hitungLamaHilalMukuts, hitungAzimut..., hitungJarakBusurElongasi
val sbs = if (sbsh > 90.0) {
    90.0 + nishfulFadhlahBulan.nfDerajat - parallaxNishfulFadhlah.pnfDerajat + koreksiTepiAtas
} else {
    90.0 + nishfulFadhlahBulan.nfDerajat + parallaxNishfulFadhlah.pnfDerajat - koreksiTepiAtas
}

lamaHilalJam = (setengahBusurSiangBulan.sbsDerajat - sudutWaktuBulanGhurub.sudutWaktuBulanDerajat) / 15.0

val argumenTangen = (-sinDeg(lintang) / tanDeg(sudutWaktu)) +
    (cosDeg(lintang) * tanDeg(deklinasi) / sinDeg(sudutWaktu))

val argumenCosinus = sinDeg(deklinasiMatahari) * sinDeg(deklinasiBulan) +
    cosDeg(deklinasiMatahari) * cosDeg(deklinasiBulan) * cosDeg(arMatahari - arBulan)
```

Catatan audit: excerpt ini menunjukkan formula inti tahap 13-32. Untuk reviewer, bagian ini adalah tempat utama memeriksa kesesuaian rumus matematika dengan dokumen prosedur.

### D. Helper ephemeris, interpolasi, normalisasi, dan pembacaan nilai

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: interpolasiGhurub, interpolasiAngkaGhurub, nilaiDerajat, nilaiJam, normalisasi
private fun interpolasiGhurub(
    konteks: KonteksHisabHilal,
    ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
    saatGhurub: SaatGhurub,
    tabel: TabelEphemeris,
    kolom: String,
    mode: ModeInterpolasi,
): InterpolasiEphemerisFalak {
    val jamGhurubUt = saatGhurub.waktuLokal.jamDesimal - konteks.markaz.zonaWaktu.offsetJam
    val waktuGhurubUt = normalisasiWaktu(saatGhurub.waktuLokal.tanggal, jamGhurubUt, "GMT/UT")
    val jamAtasUt = floor(waktuGhurubUt.jamDesimal).toInt()
    val jamBawahUt = jamAtasUt + 1
    val nc = waktuGhurubUt.jamDesimal - jamAtasUt
    val barisAtas = barisEphemeris(ephemerisPerTanggal, waktuGhurubUt.tanggal, jamAtasUt, tabel)
    val barisBawah = barisEphemeris(ephemerisPerTanggal, waktuGhurubUt.tanggal, jamBawahUt, tabel)
    val nilaiAtas = nilaiDerajat(barisAtas.tanggal, barisAtas.jamUt, tabel, kolom, barisAtas.row)
    val nilaiBawah = nilaiDerajat(barisBawah.tanggal, barisBawah.jamUt, tabel, kolom, barisBawah.row)
    val hasil = when (mode) {
        ModeInterpolasi.LINEAR -> nilaiAtas.nilai - (nilaiAtas.nilai - nilaiBawah.nilai) * nc
        ModeInterpolasi.SUDUT_MAJU -> normalisasiDerajat(nilaiAtas.nilai + deltaMajuDerajat(nilaiAtas.nilai, nilaiBawah.nilai) * nc)
    }
    return InterpolasiEphemerisFalak(
        jamAtasUt = barisAtas.jamUt,
        jamBawahUt = barisBawah.jamUt,
        nc = nc,
        nilaiAtas = nilaiAtas,
        nilaiBawah = nilaiBawah,
        hasilDerajat = hasil,
    )
}

private fun nilaiDerajat(
    tanggal: LocalDate,
    jamUt: Int,
    tabel: TabelEphemeris,
    kolom: String,
    row: JsonObject,
): NilaiEphemerisFalak {
    val obj = row[kolom]?.jsonObjectOrNull() ?: error("Kolom $kolom tidak tersedia pada ${tabel.label} jam $jamUt GMT/UT.")
    val value = obj.doubleAt("decimal_degree") ?: error("Nilai decimal_degree kolom $kolom tidak tersedia.")
    return NilaiEphemerisFalak(
        nilai = value,
        raw = obj.textAt("raw"),
        sumber = SumberEphemerisFalak(tanggal, jamUt, tabel.label, kolom, obj.textAt("raw"))
    )
}

private fun nilaiJam(
    tanggal: LocalDate,
    jamUt: Int,
    tabel: TabelEphemeris,
    kolom: String,
    row: JsonObject,
): NilaiEphemerisFalak {
    val obj = row[kolom]?.jsonObjectOrNull() ?: error("Kolom $kolom tidak tersedia pada ${tabel.label} jam $jamUt GMT/UT.")
    val value = obj.doubleAt("hours") ?: error("Nilai hours kolom $kolom tidak tersedia.")
    return NilaiEphemerisFalak(
        nilai = value,
        raw = obj.textAt("raw"),
        sumber = SumberEphemerisFalak(tanggal, jamUt, tabel.label, kolom, obj.textAt("raw"))
    )
}
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalEphemerisCalculator.kt
// Fungsi: normalisasiWaktu, normalisasiDerajat, trig derajat
private fun normalisasiWaktu(tanggalAwal: LocalDate, jam: Double, zona: String): WaktuFalak {
    val days = floor(jam / 24.0).toLong()
    var normalizedHour = jam - (days * 24.0)
    var date = tanggalAwal.plusDays(days)
    if (normalizedHour < 0.0) {
        normalizedHour += 24.0
        date = date.minusDays(1)
    }
    return WaktuFalak(date, normalizedHour, zona)
}

private fun deltaMajuDerajat(awal: Double, setelah: Double): Double {
    var delta = setelah - awal
    while (delta < 0.0) delta += 360.0
    return delta
}

private fun selisihSudutBertanda(nilaiKiri: Double, nilaiKanan: Double): Double {
    var delta = nilaiKiri - nilaiKanan
    while (delta > 180.0) delta -= 360.0
    while (delta <= -180.0) delta += 360.0
    return delta
}

private fun normalisasiDerajat(value: Double): Double {
    var normalized = value % 360.0
    if (normalized < 0.0) normalized += 360.0
    return normalized
}

private fun sinDeg(value: Double): Double = sin(value * PI / 180.0)

private fun cosDeg(value: Double): Double = cos(value * PI / 180.0)

private fun tanDeg(value: Double): Double = tan(value * PI / 180.0)
```

Catatan audit: helper ini adalah bagian kritis untuk menghindari error pada kasus batas. Jika helper berubah, hasil banyak tahap ikut berubah.

### E. Model audit dan konteks

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/domain/falak/HisabHilalModels.kt
// Class: ZonaWaktuFalak, MarkazFalak, KonteksHisabHilal, ButirPerhitunganFalak
data class ZonaWaktuFalak(
    val nama: String,
    val offsetJam: Double,
    val bujurStandarDerajat: Double,
) {
    companion object {
        val WIB = ZonaWaktuFalak("WIB", 7.0, 105.0)
        val WITA = ZonaWaktuFalak("WITA", 8.0, 120.0)
        val WIT = ZonaWaktuFalak("WIT", 9.0, 135.0)
    }
}

data class MarkazFalak(
    val nama: String,
    val lintangDerajat: Double,
    val bujurDerajat: Double,
    val elevasiMeter: Double,
    val zonaWaktu: ZonaWaktuFalak = ZonaWaktuFalak.WIB,
)

data class KonteksHisabHilal(
    val bulanHijriah: String,
    val tanggalSituasiHilalMasehi: LocalDate,
    val markaz: MarkazFalak,
    val jamGhurubPerkiraanLokal: Double = 18.0,
    val kriteriaAwalBulan: KriteriaAwalBulanFalak = KriteriaAwalBulanFalak.KemenagMabimsTerbaru,
)

data class ButirPerhitunganFalak(
    val nomor: Int,
    val judul: String,
    val rumus: String,
    val substitusi: String,
    val hasil: String,
    val catatan: String? = null,
    val sumber: List<SumberEphemerisFalak> = emptyList(),
)
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/model/falak/FalakModels.kt
// Class: FalakEphemerisHarian, FalakHourlyTable
@Serializable
data class FalakEphemerisHarian(
    val page: Int? = null,
    val date: String,
    @SerialName("has_structured_hourly_table") val hasStructuredHourlyTable: Boolean = false,
    @SerialName("hourly_table") val hourlyTable: FalakHourlyTable = FalakHourlyTable(),
    @SerialName("raw_text") val rawText: String? = null,
)

@Serializable
data class FalakHourlyTable(
    val sun: List<JsonObject> = emptyList(),
    val moon: List<JsonObject> = emptyList(),
)
```

### F. Repository dan pembentukan konteks UI

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/HisabHilalRepository.kt
// Fungsi: persiapkanEphemeris, hitung
override suspend fun persiapkanEphemeris(konteks: KonteksHisabHilal): Result<DataEphemerisHisabHilal> =
    runCatching {
        val tanggal = konteks.tanggalSituasiHilalMasehi
        val tanggalDiperlukan = setOf(tanggal, tanggal.plusDays(1))
        val paketPerTahun = tanggalDiperlukan
            .map { it.year }
            .distinct()
            .map { tahun ->
                falakRepository.loadDataLengkap(tahun).recoverCatching {
                    falakRepository.refreshPaketKemenag(tahun).getOrThrow()
                }.getOrThrow()
            }
        val ephemeris = paketPerTahun.flatMap { data ->
            data.ephemerisHarian.filter { item ->
                runCatching { LocalDate.parse(item.date) }.getOrNull() in tanggalDiperlukan
            }
        }
        val tersedia = ephemeris.mapTo(mutableSetOf()) { LocalDate.parse(it.date) }
        val hilang = tanggalDiperlukan - tersedia
        check(hilang.isEmpty()) {
            "Data ephemeris tanggal ${hilang.joinToString()} belum tersedia pada paket ${paketPerTahun.joinToString { it.paket.kode }}."
        }
        DataEphemerisHisabHilal(
            paketUtama = paketPerTahun.first(),
            paketPendukung = paketPerTahun.drop(1),
            tanggalSituasiHilalMasehi = tanggal,
            ephemerisHarian = ephemeris.distinctBy { it.date }.sortedBy { it.date },
        )
    }

override suspend fun hitung(konteks: KonteksHisabHilal): Result<HasilHisabHilalEphemeris> =
    runCatching {
        val siap = persiapkanEphemeris(konteks).getOrThrow()
        calculator.hitung(konteks, siap.ephemerisHarian)
    }
```

```kotlin
// Nama file: app/src/main/java/com/alhasanah/alhasanahmedia/ui/falak/HisabHilalViewModel.kt
// Fungsi: buildKonteks
private fun buildKonteks(state: HisabHilalUiState): KonteksHisabHilal {
    val tanggal = LocalDate.parse(state.tanggalSituasiHilal.trim())
    val input = state.markazInput
    val lintang = input.lintang.toDoubleOrNull() ?: error("Lintang belum valid.")
    val bujur = input.bujur.toDoubleOrNull() ?: error("Bujur belum valid.")
    val elevasi = input.elevasi.toDoubleOrNull() ?: error("Elevasi belum valid.")
    val zona = when (input.zona.trim().uppercase()) {
        "WIB" -> ZonaWaktuFalak.WIB
        "WITA" -> ZonaWaktuFalak.WITA
        "WIT" -> ZonaWaktuFalak.WIT
        else -> FalakMarkazProvider.zonaWaktuIndonesia(bujur)
    }
    return KonteksHisabHilal(
        bulanHijriah = state.bulanHijriah.ifBlank { "Bulan Hijriah" },
        tanggalSituasiHilalMasehi = tanggal,
        markaz = MarkazFalak(
            nama = input.nama.ifBlank { "Markaz" },
            lintangDerajat = lintang,
            bujurDerajat = bujur,
            elevasiMeter = elevasi,
            zonaWaktu = zona,
        ),
        kriteriaAwalBulan = state.kriteria,
    )
}
```

Catatan audit: repository memastikan data tanggal tersedia sebelum kalkulator dipanggil. ViewModel membentuk konteks dari input pengguna, sehingga validasi markaz dan tanggal harus diperhatikan bila audit dilakukan terhadap hasil dari UI.
