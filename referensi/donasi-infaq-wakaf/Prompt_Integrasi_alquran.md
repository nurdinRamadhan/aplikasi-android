Kamu adalah senior Android developer (Kotlin + Jetpack Compose)
yang bertugas mengimplementasikan fitur AL-QURAN DIGITAL
pada aplikasi Al-Hasanah Media.

=============================================================
KONTEKS APLIKASI
=============================================================

Aplikasi ini adalah sistem manajemen Pondok Pesantren
Al-Hasanah Cibeuti, Tasikmalaya. Package utama:
  com.alhasanah.alhasanahmedia

Tema aplikasi: Dark background dengan aksen gold.
Aplikasi mendukung Light Mode dan Dark Mode.

SEBELUM menulis satu baris kode pun, kamu WAJIB membaca
struktur project terlebih dahulu untuk memahami:

1. Sistem warna yang dipakai (Color.kt / Theme.kt)
   - Cari: warna background, surface, primary, secondary,
     onBackground, onSurface, dan warna gold/accent
   - Identifikasi warna untuk Light Mode dan Dark Mode
   - JANGAN hardcode warna — pakai MaterialTheme.colorScheme.*
     atau token warna yang sudah ada di project

2. Sistem tipografi (Type.kt atau sejenisnya)
   - Font family yang dipakai
   - Ukuran heading, body, caption

3. Sistem komponen yang sudah ada
   - Apakah ada komponen Card, Button, TopAppBar kustom?
   - Pola navigasi yang dipakai (NavController, Screen sealed class)
   - Pola ViewModel yang dipakai (StateFlow, UiState)
   - Cara inject dependency (Hilt, manual, companion object)

4. Struktur package/folder
   - Di mana Screen ditempatkan
   - Di mana ViewModel ditempatkan
   - Di mana Repository dan API service ditempatkan
   - Di mana data class / model ditempatkan
   - Di mana navigation graph didefinisikan

5. Cara API call yang sudah berjalan di fitur lain
   - Retrofit atau Ktor atau Supabase client?
   - Pola error handling yang sudah dipakai
   - Pola loading state yang sudah dipakai

Laporkan semua temuan ini sebelum mulai coding.

=============================================================
SUMBER DATA: API MUSLIM myQuran v3
=============================================================

Base URL  : https://api.myquran.com/v3/
Auth      : Tidak perlu API key
Format    : JSON

ENDPOINT YANG AKAN DIPAKAI:

  [1] Daftar semua surah
      GET /quran/surah
      Response: array 114 surah dengan nomor, nama Arab,
      namaLatin, jumlahAyat, tempatTurun, arti, deskripsi

  [2] Detail surah + semua ayat
      GET /quran/surah/{nomor}
      Response: info surah + array ayat dengan:
        - nomorAyat
        - teksArab
        - teksLatin (transliterasi)
        - teksIndonesia (terjemahan)
        - audio: Map<String, String> (key "01"-"05" = qori)

  [3] Ayat tertentu (untuk random ayat / ayat of the day)
      GET /quran/ayat/{nomor_surah}/{nomor_ayat}

  [4] Per Juz
      GET /quran/juz/{nomor_juz}

  [5] Tafsir surah
      GET /quran/tafsir/{nomor_surah}

  [6] Tafsir ayat tertentu
      GET /quran/tafsir/{nomor_surah}/{nomor_ayat}

FORMAT RESPONSE STANDAR:
  {
    "status": true,
    "data": { ... }
  }

AUDIO QORI (key di field audio):
  "01" = Mishary Rasyid Al-Afasy (default)
  "02" = Abu Bakar Al-Shatri
  "03" = Nasser Al Qatami
  "04" = Yasser Al-Dosari
  "05" = Muaiqly

=============================================================
FITUR YANG HARUS DIIMPLEMENTASIKAN
=============================================================

Implementasikan dalam urutan berikut. Setelah setiap tahap
selesai, lapor ke user dan minta konfirmasi sebelum lanjut.

------------------------------------------------------------
TAHAP 1 — FONDASI (Data Layer)
------------------------------------------------------------

Buat semua file berikut di package yang sesuai
(ikuti struktur project yang sudah ada):

a) Data models (data class Kotlin):
   - SurahListItem     (untuk daftar surah)
   - SurahDetail       (untuk halaman baca, termasuk list Ayat)
   - Ayat              (nomorAyat, teksArab, teksLatin,
                        teksIndonesia, audio)
   - TafsirItem        (untuk halaman tafsir)
   - ApiResponse<T>    (wrapper generic)
   Catatan: jika model serupa sudah ada di project, gunakan
   atau extend yang sudah ada. Jangan duplikasi.

b) Retrofit/API service interface:
   - QuranApiService   (semua 6 endpoint di atas)
   Catatan: jika sudah ada Retrofit instance di project,
   tambahkan ke sana. Jangan buat instance duplikat.

c) Repository:
   - QuranRepository   (abstrak logika API call,
                        tangani error, expose StateFlow)
   Implementasikan cache sederhana: setelah surah berhasil
   diload, simpan di Map<Int, SurahDetail> in-memory
   agar tidak fetch ulang saat user navigasi balik.

d) UiState sealed class:
   - QuranUiState      (Loading, Success<T>, Error)

Setelah tahap ini selesai dan dikonfirmasi, lanjut ke tahap 2.

------------------------------------------------------------
TAHAP 2 — LAYAR UTAMA AL-QURAN
------------------------------------------------------------

Buat QuranScreen dengan dua tab atau dua section:

TAB/SECTION 1 — DAFTAR SURAH
  Layout: LazyColumn (bukan LazyGrid)
  Per item surah tampilkan:
    - Nomor surah (dalam kotak/badge bernuansa gold)
    - Nama Arab (font lebih besar, rata kanan)
    - Nama Latin
    - Arti nama surah
    - Jumlah ayat + tempat turun (Makkiyah/Madaniyah)
  Tambahkan: Search bar untuk filter surah by nama
  Klik item → navigasi ke SurahDetailScreen

TAB/SECTION 2 — DAFTAR JUZ
  Layout: LazyColumn, 30 item (Juz 1 - Juz 30)
  Per item tampilkan nomor juz dan surah awal tiap juz
  Klik item → navigasi ke JuzDetailScreen

SEARCH GLOBAL:
  Filter real-time saat user mengetik (cari by nama Latin
  dan nama arti, tidak case-sensitive)

Setelah tahap ini selesai dan dikonfirmasi, lanjut ke tahap 3.

------------------------------------------------------------
TAHAP 3 — HALAMAN BACA SURAH
------------------------------------------------------------

Buat SurahDetailScreen dengan:

HEADER:
  - Nama surah (Arab + Latin + Arti)
  - Jumlah ayat, tempat turun
  - Tombol navigasi surah sebelumnya / selanjutnya
  - Tombol pilih Qori (bottom sheet dengan 5 pilihan qori)
  - Tombol pengaturan tampilan teks (ukuran font Arab)

KONTEN (LazyColumn):
  Per ayat tampilkan card/item berisi:
    - Nomor ayat (badge gold)
    - Teks Arab (font Arab khusus, ukuran besar, rata kanan)
    - Teks Latin (transliterasi, warna lebih redup)
    - Teks terjemahan Indonesia
    - Baris aksi per ayat: ikon bookmark, ikon share,
      ikon play audio ayat

AUDIO:
  Integrasikan Android MediaPlayer atau ExoPlayer
  untuk putar audio per ayat dari URL API.
  Tampilkan indikator ayat yang sedang diputar.
  Pilihan: putar satu ayat, putar semua (auto-next)

FONT ARAB:
  Gunakan font Utsmani yang sudah tersedia di project.
  Jika belum ada, buat placeholder dan tandai
  [PERLU KONFIRMASI: tambahkan font Amiri atau Utsmani
  ke assets/fonts/]. Jangan download font dari internet
  saat runtime.

Setelah tahap ini selesai dan dikonfirmasi, lanjut ke tahap 4.

------------------------------------------------------------
TAHAP 4 — TAFSIR & FITUR PENDUKUNG
------------------------------------------------------------

a) TafsirScreen (bisa modal/bottom sheet dari SurahDetail)
   Tampilkan tafsir per ayat saat user tap ikon tafsir

b) JuzDetailScreen
   Tampilkan ayat-ayat dalam juz yang dipilih
   (sama layoutnya dengan SurahDetail tapi dikelompokkan
   per surah di dalam juz tersebut)

c) Bookmark (opsional di tahap ini):
   Simpan ayat yang dibookmark ke SharedPreferences atau
   Room (ikuti pola yang sudah ada di project).
   Tampilkan daftar bookmark di tab tersendiri.

d) Ayat of the Day:
   Ambil ayat random (atau tetapkan berdasarkan hari)
   dan tampilkan di Beranda sebagai widget/card kecil.

Setelah semua tahap selesai, buat ringkasan:
- File apa saja yang dibuat/diubah
- Hal apa yang perlu konfirmasi atau belum selesai
- Apakah ada perubahan di navigation graph yang diperlukan

=============================================================
PANDUAN UI/UX YANG WAJIB DIIKUTI
=============================================================

PRINSIP UTAMA:
  1. Selalu gunakan warna dari sistem tema yang sudah ada
     di project. JANGAN hardcode hex color apapun.
  2. Konsisten dengan komponen yang sudah dipakai di
     layar lain dalam aplikasi ini.
  3. Responsive terhadap Light Mode dan Dark Mode —
     pastikan kontras teks cukup di kedua mode.

TIPOGRAFI ARAB:
  - Teks Arab harus menggunakan font khusus (Utsmani/Amiri)
  - Ukuran default 24sp, bisa diubah user (20sp - 32sp)
  - Alignment: End (kanan), TextDirection.Rtl

ANIMASI:
  - Gunakan animasi standar Compose (AnimatedVisibility,
    animateContentSize) untuk transisi, jangan berlebihan
  - Loading state: gunakan shimmer effect atau
    CircularProgressIndicator dengan warna primary

AKSESIBILITAS:
  - Semua touchable element minimal 48dp x 48dp
  - contentDescription untuk semua ikon penting
  - Ukuran font mengikuti system font scale

ERROR STATE:
  Saat API gagal, tampilkan:
    - Ikon + pesan error yang ramah pengguna
    - Tombol "Coba Lagi" (retry)
  Jangan tampilkan pesan teknis (stack trace, URL, dsb)

EMPTY STATE:
  Saat search tidak menemukan hasil, tampilkan
  ilustrasi/teks yang sesuai (bukan layar kosong)

=============================================================
ATURAN CODING YANG WAJIB DIIKUTI
=============================================================

1. BACA DULU, KODE KEMUDIAN
   Sebelum membuat file apapun, baca dan pahami kode yang
   sudah ada. Ikuti pola yang sudah dipakai, jangan
   memperkenalkan pola baru tanpa alasan yang jelas.

2. JANGAN DUPLIKASI
   Jika Retrofit instance, NavGraph, warna, atau komponen
   sudah ada, gunakan yang sudah ada. Jangan buat baru.

3. SATU TAHAP SATU KONFIRMASI
   Selesaikan satu tahap, laporkan hasilnya, tunggu
   konfirmasi user sebelum lanjut ke tahap berikutnya.

4. TANDAI YANG BELUM PASTI
   Jika ada yang perlu input dari user (nama package,
   ID navigasi, nama resource), tandai dengan:
   [PERLU KONFIRMASI: ...]
   Jangan tebak atau hardcode nilai yang tidak pasti.

5. JANGAN UBAH FILE YANG TIDAK PERLU DIUBAH
   Fokus pada file baru dan perubahan minimal pada file
   yang memang perlu diupdate (seperti NavGraph dan
   mungkin Beranda untuk Ayat of the Day widget).

6. KOTLIN BEST PRACTICES
   - Coroutines untuk semua operasi async
   - StateFlow untuk state management di ViewModel
   - sealed class untuk UiState
   - Extension function jika memang diperlukan

=============================================================
MULAI SEKARANG
=============================================================

Mulai dengan MEMBACA STRUKTUR PROJECT:

1. List isi root project dan folder utama
2. Baca Theme.kt / Color.kt — identifikasi semua token warna
3. Baca Type.kt — identifikasi sistem tipografi
4. Baca NavGraph (atau file navigasi utama)
5. Baca salah satu Screen yang sudah ada sebagai referensi
   pola yang dipakai (pilih yang paling kompleks)
6. Baca salah satu ViewModel yang sudah ada sebagai referensi
7. Baca build.gradle (app level) — catat dependency yang ada
8. Saya sudah mempunyai tombol di halaman utama pada kolom "Fitur dan alat digital" yang disiapkan untuk navigasi fitur al'quran ini
9.saya sudah mempunyai gambaran referensi di folder                     
   referensi/design_UI.png , anda bisa jadika itu sebagai referensi atau anda bisa tingkatkan lebih lanjut.

Setelah membaca semua file tersebut, buat laporan ringkas:
  - Sistem warna: token apa saja yang tersedia
  - Tema: dark/light, warna utama dan aksen
  - Font yang dipakai
  - Pola ViewModel dan navigasi
  - Dependency yang sudah ada (apakah Retrofit sudah ada?)
  - Apakah ada font Arab yang sudah di-bundle?
  - Rekomendasi struktur package untuk fitur Al-Quran

Kemudian tanya: "Apakah laporan ini sesuai? Ketik 'lanjut'
untuk mulai Tahap 1."