# Rencana Project Visualisasi Falak

Dokumen ini menjadi acuan sebelum implementasi fitur visualisasi Falak di aplikasi Android. Fokus awal adalah visualisasi hilal dan gerhana bulan berdasarkan hasil hisab ephemeris yang sudah ada, lalu diperluas ke deteksi otomatis dan prediksi lintas tahun.

## Tujuan

1. Menampilkan hasil hisab Falak secara visual, bukan hanya tabel angka.
2. Tetap mempertahankan sumber resmi Kemenag sebagai jalur utama ketika data ephemeris tersedia.
3. Menyediakan jalur pembanding Jean Meeus untuk prediksi lintas tahun, pencarian kandidat, dan edukasi.
4. Memisahkan visualisasi dari kalkulator agar rumus tetap mudah diaudit.
5. Menyajikan visual yang berguna untuk umum, santri, pengajar, dan praktisi tanpa menghilangkan detail astronomis.

## Prinsip Arsitektur

Perhitungan dan visualisasi harus dipisah:

- `domain/falak`: kalkulator, model hasil, deteksi kandidat, pembanding astronomis.
- `data/repository`: pengambilan ephemeris Kemenag, cache lokal, fallback data.
- `ui/falak`: halaman, tab, canvas visual, kontrol waktu, dan pilihan sumber data.

Visualisasi tidak boleh menghitung ulang rumus utama secara tersembunyi. Visual harus membaca model hasil yang sudah dihitung, lalu mengubahnya menjadi koordinat, fase, bayangan, dan timeline.

## Sumber Perhitungan

### 1. Ephemeris Kemenag

Dipakai sebagai sumber utama untuk:

- hisab awal bulan,
- posisi hilal saat ghurub,
- gerhana bulan berbasis tabel resmi,
- audit terhadap dokumen Kemenag/pesantren.

Kelebihan:

- cocok untuk pembelajaran dan audit resmi,
- mudah dibandingkan dengan PDF Kemenag,
- seluruh nilai dapat ditelusuri ke baris tanggal, jam, tabel, dan kolom ephemeris.

Keterbatasan:

- hanya berlaku pada tahun yang datanya tersedia,
- jika Kemenag belum merilis PDF tahun depan, aplikasi tidak bisa mengandalkan jalur ini.

### 2. Jean Meeus

Dipakai sebagai jalur pembanding dan prediksi lintas tahun:

- mencari purnama atau ijtimak terdekat,
- mendeteksi kandidat gerhana bulan beberapa tahun ke depan,
- memberi estimasi awal sebelum data Kemenag tersedia,
- membandingkan selisih hasil Meeus dan ephemeris Kemenag saat data resmi sudah tersedia.

Catatan:

- jalur Meeus tidak menggantikan hasil resmi Kemenag,
- di UI harus diberi label jelas seperti `Pembanding Meeus` atau `Prediksi Meeus`,
- untuk gerhana matahari, Meeus harus dilengkapi koreksi lokasi/toposentrik sebelum dianggap memadai untuk praktisi.

## Mode Data

### Mode Kemenag

Alur:

1. User memilih data/acuan dari paket Kemenag.
2. Repository mengambil ephemeris dari cache lokal atau Supabase Storage.
3. Kalkulator menjalankan rumus resmi berbasis data ephemeris.
4. UI menampilkan hasil angka, sumber ephemeris, dan visual.

Validasi:

- data H-1, H, H+1 harus tersedia untuk acuan gerhana bulan,
- data tambahan boleh disiapkan untuk interpolasi,
- jika data tidak lengkap, tampilkan pesan sinkronisasi data.

### Mode Input Manual

Alur:

1. User memasukkan tanggal sekitar kejadian.
2. User memilih rentang pencarian, misalnya `H±2`, `H±3`, atau `H±5`.
3. Aplikasi mencari FIB terbesar atau fase terkait di rentang tersebut.
4. Jika geometri tidak memenuhi syarat, aplikasi tetap menampilkan rincian dan kesimpulan `Tidak terjadi gerhana`.

Validasi:

- jangan gagal hanya karena tanggal input bukan tanggal gerhana,
- error hanya untuk data ephemeris yang benar-benar tidak tersedia atau rusak,
- tampilkan catatan apakah tanggal jauh dari purnama/ijtimak atau jauh dari simpul Bulan.

### Mode Prediksi Meeus

Alur:

1. User memilih rentang tahun, misalnya 2, 5, atau 10 tahun ke depan.
2. Kalkulator Meeus mencari kandidat purnama/ijtimak dan kedekatan simpul.
3. Kandidat ditampilkan dalam daftar.
4. Jika paket Kemenag tersedia untuk tahun kandidat, aplikasi dapat membuka hasil berbasis Kemenag.
5. Jika paket Kemenag belum tersedia, aplikasi menampilkan status `Prediksi Meeus`.

## Visualisasi Yang Dibangun

### Tahap 1: Visual Gerhana Bulan 2D

Tampilan:

- lingkaran penumbra,
- lingkaran umbra,
- piringan Bulan,
- lintasan Bulan,
- titik kontak awal, tengah, total, selesai,
- timeline interaktif.

Data dari kalkulator:

- jenis gerhana,
- waktu kontak,
- magnitude,
- lintang Bulan,
- semi diameter Bulan,
- semi diameter bayangan inti,
- jarak kontak,
- simpul dan nilai koreksi.

Interaksi:

- slider waktu,
- tombol langkah kontak,
- pilihan zona waktu,
- toggle `Tampilkan sumber data`,
- toggle `Tampilkan nilai geometri`.

Catatan:

- 2D cukup untuk audit dan pembelajaran,
- ukuran visual tidak harus skala fisik absolut, tetapi rasio bayangan dan lintasan harus konsisten dengan hasil hitung.

### Tahap 2: Visual Hilal 2D

Tampilan:

- garis ufuk barat,
- posisi Matahari di bawah ufuk,
- posisi Bulan/hilal,
- bentuk fase hilal,
- arah elongasi,
- label tinggi hilal,
- label elongasi,
- umur Bulan,
- FIB/Nurul Hilal,
- status kriteria Kemenag/MABIMS atau kriteria lain.

Data dari kalkulator:

- tinggi hilal mar'i tepi atas,
- azimuth Bulan dan Matahari,
- elongasi,
- umur Bulan,
- FIB/Nurul Hilal,
- waktu ghurub,
- status kriteria.

Interaksi:

- pilih markaz,
- deteksi lokasi,
- input manual lintang/bujur/elevasi,
- slider sekitar waktu ghurub,
- toggle kriteria.

### Tahap 3: Deteksi Otomatis Gerhana Bulan

Tampilan:

- daftar kandidat gerhana,
- filter tahun,
- filter jenis gerhana,
- label sumber `Kemenag`, `Meeus`, atau `Bandingkan`,
- tombol buka visual.

Alur:

1. Meeus mencari purnama dekat simpul.
2. Kandidat diklasifikasi awal.
3. Jika ephemeris Kemenag tersedia, hitung ulang dengan jalur resmi.
4. Simpan hasil kandidat sementara di cache aplikasi.

### Tahap 4: Visual Gerhana Matahari

Tahap ini dikerjakan setelah gerhana bulan dan hilal stabil.

Kebutuhan tambahan:

- koordinat markaz yang presisi,
- topocentric correction,
- parallax Bulan,
- altitude/azimuth Matahari dan Bulan,
- besar piringan tampak Matahari dan Bulan,
- status visibilitas dari lokasi user.

Tampilan awal:

- piringan Matahari,
- piringan Bulan yang menutup Matahari,
- persentase tertutup,
- waktu kontak lokal jika dapat dihitung.

## Library Yang Diperlukan

### Wajib Untuk Tahap Awal

Tidak perlu library grafis tambahan.

- Jetpack Compose Canvas
  - menggambar hilal, umbra, penumbra, lintasan, piringan Matahari/Bulan.
- Jetpack Compose Animation
  - slider, transisi kontak, animasi waktu.
- Material 3
  - kontrol, tab, chip, card, dropdown.
- `java.time`
  - tanggal, waktu, zona.

### Opsional Untuk Tahap Lanjutan

- Filament
  - visualisasi 3D Bumi-Bulan-Matahari native Android.
- SceneView
  - wrapper Filament agar 3D lebih cepat dikembangkan.
- OSMDroid atau MapLibre
  - memilih markaz dari peta jika nanti ingin visual gerhana matahari berbasis lokasi.
- Open-Meteo Elevation API
  - mengambil elevasi lokasi otomatis, tetap harus ada input manual.

### Tidak Disarankan Untuk Tahap Awal

- Three.js di WebView
  - bisa dipakai, tetapi kurang ideal untuk aplikasi Android native jika hanya butuh visual 2D.
- Library astronomi besar tanpa audit
  - hindari memasukkan library yang hasilnya tidak mudah dijelaskan ke santri/praktisi.

## Struktur File Yang Disarankan

Domain:

- `domain/falak/VisualFalakModels.kt`
- `domain/falak/VisualGerhanaBulanMapper.kt`
- `domain/falak/VisualHilalMapper.kt`
- `domain/falak/DeteksiGerhanaBulanMeeus.kt`

UI:

- `ui/falak/VisualFalakScreen.kt`
- `ui/falak/VisualGerhanaBulanCanvas.kt`
- `ui/falak/VisualHilalCanvas.kt`
- `ui/falak/VisualTimelineControls.kt`

Test:

- `VisualGerhanaBulanMapperTest.kt`
- `VisualHilalMapperTest.kt`
- `DeteksiGerhanaBulanMeeusTest.kt`

## Urutan Implementasi

### Langkah 1: Model Visual Gerhana Bulan

Membuat model koordinat visual:

- pusat umbra,
- radius umbra,
- radius penumbra,
- radius Bulan,
- lintasan Bulan,
- posisi kontak,
- posisi berdasarkan waktu slider.

Output tahap ini harus bisa diuji unit test tanpa UI.

### Langkah 2: Canvas Gerhana Bulan

Membuat Composable Canvas:

- menggambar umbra,
- menggambar penumbra,
- menggambar Bulan,
- menggambar lintasan,
- menampilkan label kontak.

### Langkah 3: Timeline Gerhana Bulan

Menambahkan:

- slider waktu,
- tombol `Awal`, `Total`, `Tengah`, `Selesai`,
- animasi sederhana.

### Langkah 4: Visual Hilal

Membuat model dan canvas hilal:

- ufuk,
- posisi Bulan,
- posisi Matahari,
- fase hilal,
- garis elongasi,
- label kriteria.

### Langkah 5: Deteksi Otomatis Gerhana Bulan

Membuat detektor Meeus:

- input rentang tahun,
- output daftar kandidat,
- status kemungkinan,
- opsi hitung ulang dengan Kemenag jika data tersedia.

### Langkah 6: Integrasi Tab Visual

Menambahkan tab `Visual` di halaman `Falaq Ephemeris`.

Isi tab:

- tombol `Visual Gerhana Bulan`,
- tombol `Visual Hilal`,
- tombol `Deteksi Gerhana`,
- label status sumber data.

### Langkah 7: QA

Membuat QA:

- visual gerhana bulan terhadap hasil kalkulator,
- visual hilal terhadap hisab awal bulan,
- toleransi posisi visual,
- catatan bahwa visual adalah representasi terukur dari hasil hisab, bukan pengganti tabel audit.

## Kriteria Selesai

Fitur dianggap siap uji device jika:

1. Semua hasil visual mengambil data dari kalkulator yang sudah diuji.
2. Tidak ada rumus utama tersembunyi di Composable.
3. Unit test mapper visual lulus.
4. UI bisa dipakai tanpa login.
5. Teks sumber data jelas: Kemenag, Meeus, atau Bandingkan.
6. Jika data Kemenag tidak tersedia, aplikasi tidak mengklaim hasil sebagai resmi.
7. Visual tetap terbaca di layar kecil.

