Plan Migrasi Header Global

  Tujuan: semua halaman selain Home memakai header berbasis dark_halaman.png /
  light_halaman.png, tanpa header Canvas/gradient lama yang tersisa. Home tetap memakai
  dark_header.png / light_header.png.

  1. Tetapkan Komponen Header Tunggal
  Buat/sempurnakan komponen global:

  - AppPageHeaderBackground
  - AppPageHeader
  - AppPageHeaderActionButton

  Standar isi:

  - otomatis pilih dark_halaman.png atau light_halaman.png
  - tinggi default konsisten, misalnya 184.dp untuk halaman biasa
  - varian tinggi: compact, standard, large
  - mendukung tombol back, title, subtitle, action kanan
  - tidak memakai Canvas
  - teks aman untuk dark/light
  - background fallback #1F2326 untuk dark dan putih hangat untuk light

  2. Inventarisasi Semua Halaman
  Kelompokkan halaman dari AppNavHost:

  Sudah sebagian:

  - Home
  - Keuangan
  - Donasi
  - Kesehatan
  - Pelanggaran
  - Perizinan
  - Hafalan
  - Hafalan Kitab
  - Detail Santri

  Belum menyeluruh:

  - Murajaah
  - Prestasi
  - Santri List
  - Santri Activity
  - Quran
  - Surah Detail
  - Juz Detail
  - Hadith
  - Hadith Detail
  - Devotion / Kitab Kuning
  - Ibadah Guide
  - Prayer Schedule
  - Islamic Calendar
  - Qibla
  - Weather
  - Falak Ephemeris
  - Hisab Hilal
  - Gerhana Bulan
  - Berita List
  - Berita Detail
  - Notification
  - Login
  - Payment Instruction
  - Payment Result
  - Wallet Wali
  - Wallet Kantin
  - Wallet Dispute
  - Alumni screens
  - RAG Chat
  - Admin Panel
  - Splash

  3. Audit Header Lama
  Cari semua pola lama:

  - Canvas di area header
  - drawBehind { drawArc(...) }
  - Brush.verticalGradient(...) untuk header
  - HomeHeroOrnament
  - QuranGradientHeader
  - DevotionGradientHeader
  - WeatherHeader
  - QiblaHeader
  - PrestasiHeader
  - MurajaahHeader
  - semua fungsi bernama *Header, *TopBar, *GradientHeader, *HeroHeader

  Output audit harus berupa checklist per file:

  - header canvas ada/tidak
  - sudah pakai AppPageHeader atau belum
  - block/card sudah pakai warna global atau belum

  4. Migrasi Bertahap Per Modul
  Urutan aman:

  1. Modul Santri:
      - MurajaahScreen
      - PrestasiScreen
      - SantriListScreen
      - SantriActivityScreen

  2. Modul Keuangan/Transaksi:
      - KeuanganScreen
      - DonasiScreen
      - WalletScreens
      - PaymentInstructionScreen
      - PaymentResultScreen

  3. Modul Islamic tools:
      - QuranScreen
      - SurahDetailScreen
      - JuzDetailScreen
      - HadithScreen
      - DevotionScreen
      - IbadahGuideScreen
      - PrayerScheduleScreen
      - IslamicCalendarScreen
      - QiblaScreen

  4. Modul Informasi:
      - BeritaListScreen
      - BeritaDetailScreen
      - NotificationScreen
      - WeatherScreen

  5. Modul Falak:
      - FalakEphemerisScreen
      - HisabHilalScreen
      - GerhanaBulanScreen

  6. Modul Alumni:
      - ForumAlumniScreen
      - AlumniChatScreen
      - AlumniDirectoryScreen
      - AlumniRegisterScreen
      - AlumniProfileScreen
      - AlumniSettingsScreen

  7. Auth/Admin:
      - LoginScreen
      - AdminPanelScreen
      - SplashScreen

  5. Aturan Migrasi Tiap Halaman
  Untuk setiap halaman:

  - hapus Canvas header lama
  - hapus gradient header lama
  - ganti root header dengan AppPageHeader
  - pertahankan title/subtitle/icon/action yang penting
  - jangan ubah business logic
  - jangan ubah navigasi
  - jangan ubah card/list kecuali warna bentrok
  - compile setelah batch kecil

  6. Standarisasi Block Kolom
  Setelah header konsisten, semua card/block pindah ke token global:

  - appPanelColor(isDark)
  - appPanelVariantColor(isDark)
  - appPanelBorderColor(isDark)

  Target:

  - tidak ada warm-brown/cream yang muncul di dark mode
  - tidak ada background pink/merah di light mode
  - semua card terlihat menyatu dengan charcoal/light header

  7. Validasi Teknis
  Setelah tiap batch:

  - ./gradlew :app:compileDebugKotlin

  Setelah semua selesai:

  - ./gradlew :app:lintRelease
  - ./gradlew assembleDebug
  - idealnya cek visual manual di:
      - dark mode
      - light mode
      - halaman dengan data kosong
      - halaman dengan data panjang
      - halaman detail/modal/bottom sheet

  8. Definition of Done
  Selesai jika:

  - tidak ada header Canvas lama tersisa
  - semua halaman internal memakai dark_halaman.png / light_halaman.png
  - Home hanya memakai dark_header.png / light_header.png
  - semua block/card utama memakai token panel konsisten
  - compile sukses
  - tidak ada header terlalu tinggi atau teks overlap
  - dark mode tidak lagi warm-brown/kuning
  - light mode tidak lagi pink/merah/cream berlebihan
