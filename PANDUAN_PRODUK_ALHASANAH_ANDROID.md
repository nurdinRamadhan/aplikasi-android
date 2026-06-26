# PANDUAN PRODUK AL-HASANAH MEDIA ANDROID

**Versi dokumen:** 3.0  
**Tanggal pembaruan:** 23 Mei 2026  
**Minimum Android:** Android 10.0 (API 29)  
**Ditujukan untuk:** wali santri, alumni, petugas kantin, dan pengguna aplikasi Android Al-Hasanah Media

---

## 1. Ringkasan Produk

Al-Hasanah Media Android adalah aplikasi pendamping untuk memantau aktivitas santri, mengakses layanan pesantren, melakukan pembayaran, memakai Dompet Santri, mengikuti informasi pesantren, dan menggunakan fitur keislaman digital.

Aplikasi ini bukan Admin Panel. Pengguna Android tidak menginput data administrasi pesantren seperti data induk santri, tagihan, hafalan, pelanggaran, perizinan, kesehatan, atau berita. Data tersebut dibuat dan diperbarui oleh pengurus melalui sistem resmi pesantren/backend. Aplikasi Android berfungsi sebagai kanal pemantauan, pembayaran, komunikasi terbatas, dan layanan digital untuk pengguna yang berwenang.

### 1.1 Fungsi Utama Android

- Wali santri memantau profil santri, hafalan, catatan kedisiplinan, kesehatan, perizinan, tagihan, notifikasi, dan Dompet Santri.
- Alumni memakai registrasi alumni, profil, direktori, forum, follow, komentar, reaction, laporan konten, dan chat terenkripsi.
- Petugas kantin memakai fitur merchant untuk transaksi Dompet Santri berbasis QR/NFC dan PIN santri sesuai alur.
- Pengguna umum dapat mengakses fitur publik seperti berita, Al-Quran digital, jadwal sholat, cuaca, arah kiblat, kalender Islam, hadis, panduan ibadah, donasi, dan Tanya AI sesuai akses yang tersedia.

### 1.2 Batas Wewenang Pengguna Android

Wali santri dapat melihat data anak yang terhubung dengan akunnya, tetapi tidak dapat mengubah data akademik, kesantrian, kesehatan, atau tagihan. Aksi wali yang aktif hanya berada pada area pembayaran, donasi, Dompet Santri, pengaturan limit dompet, approval transaksi tertentu, dispute dompet, notifikasi perangkat, dan interaksi yang memang disediakan oleh aplikasi.

Alumni dapat mengelola profil dan berinteraksi di fitur alumni setelah verifikasi. Petugas kantin hanya dapat memakai fitur merchant sesuai perangkat dan role yang diberikan. Jika ada data yang salah, pengguna harus menghubungi pengurus pesantren, bukan mengubahnya dari aplikasi Android.

---

## 2. Keamanan dan Privasi

Aplikasi memuat data pribadi, data santri, data keuangan, notifikasi, dan riwayat aktivitas. Karena itu, akun dan perangkat harus dijaga dengan serius.

### 2.1 Prinsip Keamanan Pengguna

1. Jangan membagikan akun aplikasi kepada orang lain.
2. Jangan membagikan OTP, PIN Dompet Santri, PIN transaksi kantin, atau kredensial login.
3. Jangan mengambil atau menyebarkan screenshot data santri tanpa alasan yang sah.
4. Jika perangkat hilang, segera hubungi pengurus agar sesi/perangkat dapat diamankan.
5. Pastikan aplikasi diunduh dari sumber resmi yang disediakan pesantren.

### 2.2 Data yang Bersifat Sensitif

Data berikut harus diperlakukan sebagai rahasia:

- Identitas santri, wali, dan alumni.
- Riwayat kesehatan, perizinan, kedisiplinan, dan hafalan.
- Tagihan, pembayaran, saldo, mutasi, QR/NFC, PIN, dan dispute Dompet Santri.
- Notifikasi pribadi.
- Isi chat alumni.

Chat alumni dirancang dengan enkripsi end-to-end. Artinya, isi chat tidak dimaksudkan untuk dibaca oleh server atau pihak lain di luar peserta percakapan.

### 2.3 Offline-First dan Cache Lokal

Beberapa data dapat tetap tampil saat koneksi tidak stabil karena aplikasi menyimpan cache lokal di private storage perangkat. Data sensitif yang memerlukan perlindungan disimpan dengan mekanisme keamanan perangkat, termasuk Android Keystore dan enkripsi sesuai implementasi aplikasi.

Modul keuangan seperti tagihan SPP, pembayaran Midtrans, top up Dompet Santri, dan transaksi yang membutuhkan konfirmasi server tetap bergantung pada koneksi online agar statusnya valid.

---

## 3. Login dan Navigasi

### 3.1 Jenis Pengguna

- **Wali santri:** masuk untuk memantau data anak, membayar tagihan, mengelola Dompet Santri, menerima notifikasi, dan memakai layanan publik.
- **Alumni:** masuk untuk mengelola profil alumni, forum, direktori, dan chat alumni.
- **Kantin:** masuk untuk menerima pembayaran Dompet Santri melalui perangkat merchant.
- **Pengguna umum:** dapat membuka fitur publik yang tidak memerlukan hubungan wali atau role khusus.

### 3.2 Menu Utama Aplikasi

Menu yang tersedia dapat berbeda tergantung status login dan role akun. Secara umum aplikasi menyediakan:

- Beranda
- Tanya AI
- Profil Santri
- Progres Hafalan
- Hafalan Kitab
- Catatan Kedisiplinan
- Rekam Medis
- Izin Santri
- Tagihan dan SPP
- Dompet Santri
- Donasi
- Berita
- Al-Quran Digital
- Jadwal Sholat
- Arah Kiblat
- Cuaca
- Kalender Islam
- Hadis
- Panduan Ibadah
- Forum dan Chat Alumni
- Notifikasi

---

## 4. Beranda

Beranda adalah pusat ringkasan. Pengguna dapat melihat akses cepat ke fitur penting, informasi santri, ringkasan tagihan atau keuangan jika tersedia, jadwal sholat, berita, notifikasi, dan fitur layanan pesantren.

Jika data belum muncul, kemungkinan akun belum terhubung dengan santri, koneksi sedang bermasalah, atau data belum tersedia dari sistem pesantren.

---

## 5. Profil dan Aktivitas Santri

Fitur santri bersifat monitoring untuk wali. Data ditampilkan berdasarkan hubungan wali-santri yang sudah terdaftar di backend.

### 5.1 Profil Santri

Profil santri menampilkan informasi dasar santri yang dapat dipantau wali, seperti identitas, kelas, dan informasi lain sesuai hak akses. Jika ada data yang salah, wali perlu menghubungi pengurus pesantren.

### 5.2 Progres Hafalan

Progres hafalan menampilkan perkembangan hafalan Al-Quran, termasuk ringkasan dan riwayat yang tersedia. Wali tidak menginput atau mengubah setoran hafalan dari aplikasi Android.

### 5.3 Hafalan Kitab

Hafalan kitab menampilkan riwayat dan capaian hafalan kitab sesuai data pesantren. Data ini bersifat baca saja untuk wali.

### 5.4 Catatan Kedisiplinan

Menu ini menampilkan catatan kedisiplinan atau pelanggaran santri jika ada. Wali dapat memantau informasi tersebut, tetapi pencatatan dan koreksi data dilakukan oleh pengurus pesantren.

### 5.5 Rekam Medis

Menu rekam medis menampilkan informasi kesehatan yang tersedia untuk wali. Data kesehatan bersifat sensitif dan tidak boleh disebarluaskan.

### 5.6 Izin Santri

Menu izin santri menampilkan riwayat perizinan yang tercatat. Pengajuan atau perubahan status izin mengikuti kebijakan dan alur resmi pesantren.

---

## 6. Tagihan, SPP, dan Pembayaran

Menu Tagihan dan SPP menampilkan daftar tagihan santri, nominal, status pembayaran, dan detail tagihan.

### 6.1 Melihat Tagihan

1. Buka menu **Tagihan dan SPP**.
2. Pilih tagihan yang ingin dilihat.
3. Periksa detail tagihan, nominal, dan status.

Wali tidak dapat membuat, mengubah nominal, atau menandai tagihan sebagai lunas secara manual.

### 6.2 Membayar Tagihan

1. Buka detail tagihan.
2. Pilih **Bayar**.
3. Pilih metode pembayaran.
4. Tekan **Lanjutkan Pembayaran**.
5. Ikuti instruksi pembayaran yang tampil.

Aplikasi memakai pembayaran digital melalui Midtrans. Setelah pembayaran berhasil dan webhook/server mengonfirmasi status, aplikasi akan memperbarui status secara otomatis dan menampilkan hasil pembayaran melalui layar hasil pembayaran yang tersedia.

### 6.3 Status Otomatis

Aplikasi memantau status pembayaran secara berkala pada layar instruksi pembayaran. Pengguna tidak perlu terus-menerus menekan tombol cek status jika pembayaran sudah dikonfirmasi server. Tombol cek status tetap tersedia sebagai aksi manual jika jaringan lambat atau pengguna ingin memastikan ulang.

---

## 7. Dompet Santri

Dompet Santri adalah fitur keuangan digital untuk kebutuhan transaksi santri di lingkungan pesantren, terutama kantin.

### 7.1 Aktivasi Dompet

Wali dapat mengaktifkan Dompet Santri dari aplikasi jika fitur tersedia untuk santri terkait. Aktivasi mengikuti validasi backend dan kebijakan pesantren.

### 7.2 PIN dan Keamanan

PIN Dompet Santri tidak boleh dibagikan. Pengurus, petugas kantin, atau pihak lain tidak boleh meminta PIN melalui chat, telepon, atau media lain. PIN hanya dimasukkan pada layar resmi aplikasi atau perangkat kantin sesuai alur.

### 7.3 Top Up

Top up dilakukan melalui alur pembayaran digital. Saldo aktif setelah pembayaran berhasil dikonfirmasi oleh server dan tercatat di ledger resmi.

### 7.4 Limit dan Approval

Wali dapat mengatur limit penggunaan sesuai fitur yang tersedia. Beberapa transaksi besar dapat memerlukan approval wali agar penggunaan saldo tetap terkendali.

### 7.5 Riwayat dan Dispute

Wali dapat melihat riwayat transaksi dan membuat dispute jika menemukan transaksi yang perlu ditinjau. Dispute akan diproses sesuai prosedur pesantren.

---

## 8. Kantin Merchant

Fitur kantin hanya untuk akun role kantin dan perangkat yang terdaftar.

Alur umum:

1. Petugas kantin masuk dengan akun yang sesuai.
2. Perangkat kantin harus aktif dan terdaftar.
3. Petugas memilih nominal transaksi.
4. Petugas membaca QR/NFC kartu santri.
5. Sistem memvalidasi dompet, saldo, limit, dan status perangkat.
6. Santri memasukkan PIN jika alur memerlukannya.
7. Jika valid, transaksi diproses dan tercatat di ledger.

Petugas kantin tidak boleh meminta atau menyimpan PIN santri di luar layar resmi.

---

## 9. Donasi

Fitur Donasi memungkinkan pengguna melakukan infaq, wakaf, atau donasi lain sesuai opsi yang tersedia.

Alur umum:

1. Pilih jenis donasi.
2. Masukkan nominal dan data yang diminta.
3. Pilih metode pembayaran.
4. Ikuti instruksi pembayaran.
5. Status pembayaran diperbarui setelah konfirmasi server.

---

## 10. Tanya AI

Tanya AI membantu pengguna bertanya seputar informasi pesantren dan pengetahuan yang tersedia di sistem. Jawaban AI bergantung pada basis pengetahuan yang disediakan dan hak akses pengguna.

### 10.1 Akses Publik dan Wali

Pengguna umum dapat memperoleh jawaban dari informasi publik. Wali yang login dapat memperoleh jawaban yang lebih relevan sesuai konteks dan data yang diizinkan.

### 10.2 Batasan Tanya AI

- Jangan memasukkan PIN, password, OTP, atau data rahasia.
- Jawaban AI tidak menggantikan keputusan resmi pengurus pesantren.
- Untuk data sensitif atau keputusan administratif, tetap hubungi pengurus.

---

## 11. Berita dan Notifikasi

### 11.1 Berita

Menu berita menampilkan informasi resmi yang dipublikasikan oleh pesantren. Pengguna dapat membaca daftar dan detail berita.

### 11.2 Notifikasi

Notifikasi dipakai untuk informasi penting, seperti tagihan, aktivitas santri, dompet, chat, atau informasi pesantren. Agar notifikasi muncul di Android 13 ke atas, pengguna harus memberikan izin notifikasi aplikasi.

---

## 12. Al-Quran Digital

Fitur Al-Quran Digital menyediakan daftar surah, detail ayat, terjemahan, tafsir jika tersedia, bookmark, dan audio murottal.

### 12.1 Audio dan Pilihan Qori

Aplikasi mendukung pilihan qori untuk audio full surah:

- Abdullah Al-Juhany
- Abdul Muhsin Al-Qasim
- Abdurrahman As-Sudais
- Ibrahim Al-Dossari
- Misyari Rasyid Al-Afasi
- Yasser Al-Dosari

Audio full surah diputar sebagai satu file MP3 agar lebih natural. Jika API utama tidak menyediakan audio lengkap untuk semua qori, aplikasi menggunakan fallback CDN eQuran yang kompatibel dengan format `audioFull`.

### 12.2 Bookmark

Pengguna dapat menandai ayat untuk dibaca kembali. Bookmark disimpan secara lokal di aplikasi.

---

## 13. Jadwal Sholat dan Pengingat

Jadwal sholat membantu pengguna melihat waktu sholat berdasarkan lokasi atau wilayah yang tersedia.

### 13.1 Lokasi

Aplikasi dapat memakai lokasi perangkat atau fallback lokasi jika izin lokasi belum tersedia. Pengguna perlu memberikan izin lokasi agar jadwal lebih sesuai.

### 13.2 Pengingat Sholat

Pengguna dapat mengaktifkan pengingat sholat. Mode yang tersedia:

- Notifikasi
- Getar
- Dering
- Adzan, menggunakan suara alarm perangkat sampai audio adzan lokal ditambahkan

Pengingat dijadwalkan berdasarkan data jadwal sholat yang tersedia dan dapat dipulihkan setelah perangkat restart atau perubahan waktu. Agar pengingat muncul, izin notifikasi perlu aktif di perangkat.

---

## 14. Cuaca

Halaman cuaca menampilkan prakiraan BMKG berdasarkan wilayah. Tampilan mendukung mode terang dan gelap, visual kondisi cuaca, animasi awan, hujan, matahari, dan petir sesuai kode cuaca.

Pengguna dapat memilih wilayah BMKG yang tersedia di katalog lokal aplikasi. Jika lokasi belum tersedia, aplikasi dapat memakai wilayah cadangan.

---

## 15. Arah Kiblat

Fitur arah kiblat membantu pengguna menentukan arah kiblat dengan sensor perangkat dan/atau data lokasi. Akurasi dapat dipengaruhi kondisi sensor, casing magnetik, dan lingkungan sekitar.

Tips:

- Gunakan di area terbuka.
- Jauhkan dari benda bermagnet.
- Kalibrasi kompas jika arah terlihat tidak stabil.

---

## 16. Kalender Islam, Hadis, dan Panduan Ibadah

Aplikasi menyediakan fitur pendukung ibadah:

- Kalender Islam dan informasi tanggal hijriah.
- Hadis dengan daftar dan detail sesuai data yang tersedia.
- Panduan ibadah, doa, dzikir, niat, dan konten keislaman lain yang tersedia di aplikasi.

Konten ini bersifat panduan. Untuk keputusan fikih yang memerlukan kehati-hatian, pengguna dapat berkonsultasi dengan ustadz atau pengurus pesantren.

---

## 17. Alumni

Fitur alumni tersedia untuk pengguna yang memiliki akses alumni.

### 17.1 Registrasi dan Profil

Alumni dapat mendaftar, mengisi profil, dan memperbarui data tertentu. Status alumni dapat memerlukan verifikasi pengurus.

### 17.2 Direktori Alumni

Direktori membantu alumni menemukan alumni lain sesuai data yang tersedia dan aturan privasi.

### 17.3 Forum Alumni

Forum alumni mendukung posting, komentar, reaction, follow, dan laporan konten. Gunakan forum dengan sopan dan hindari membagikan data pribadi tanpa izin.

### 17.4 Chat Alumni

Chat alumni memakai enkripsi end-to-end. Pengguna dapat mengirim pesan, mengarsipkan, mute, memblokir, dan memakai fitur keamanan yang tersedia. Simpan backup kunci jika aplikasi menyediakan alur tersebut agar chat terenkripsi dapat dipulihkan.

---

## 18. Troubleshooting Pengguna

### 18.1 Data Santri Tidak Muncul

Kemungkinan penyebab:

- Akun belum login.
- Akun wali belum terhubung dengan santri.
- Koneksi internet sedang bermasalah.
- Data belum tersedia dari sistem pesantren.

Solusi: refresh aplikasi, login ulang, lalu hubungi pengurus jika data tetap tidak muncul.

### 18.2 Pembayaran Belum Berubah Status

Kemungkinan penyebab:

- Pembayaran belum selesai.
- Webhook pembayaran belum diterima server.
- Koneksi perangkat lambat.
- Metode pembayaran membutuhkan waktu konfirmasi.

Solusi: tunggu beberapa saat, buka kembali instruksi pembayaran, atau gunakan tombol cek status. Jangan membayar dua kali sebelum memastikan status transaksi.

### 18.3 Notifikasi Tidak Muncul

Periksa:

- Izin notifikasi aplikasi.
- Mode hemat baterai.
- Koneksi internet.
- Pengaturan notifikasi perangkat.
- Akun masih login.

### 18.4 Audio Al-Quran Tidak Berjalan

Periksa:

- Koneksi internet.
- Volume perangkat.
- Qori yang dipilih.
- Coba hentikan lalu putar ulang.

### 18.5 Pengingat Sholat Tidak Muncul

Periksa:

- Pengingat sudah aktif.
- Izin notifikasi sudah diberikan.
- Mode baterai tidak membatasi aplikasi secara ekstrem.
- Jadwal sholat sudah berhasil dimuat.

---

## 19. Ringkasan Hak Akses Android

| Area | Wali Santri | Alumni | Kantin | Publik |
| --- | --- | --- | --- | --- |
| Profil santri | Pantau | Tidak | Tidak | Tidak |
| Hafalan, kesehatan, perizinan, pelanggaran | Pantau | Tidak | Tidak | Tidak |
| Tagihan/SPP | Pantau dan bayar | Tidak | Tidak | Tidak |
| Dompet Santri | Kelola sesuai anak | Tidak | Terima pembayaran | Tidak |
| Donasi | Ya | Ya | Ya | Ya |
| Berita | Ya | Ya | Ya | Ya |
| Al-Quran, sholat, cuaca, kiblat | Ya | Ya | Ya | Ya |
| Tanya AI | Sesuai akses | Sesuai akses | Sesuai akses | Terbatas |
| Forum dan chat alumni | Jika alumni | Ya | Tidak | Terbatas/tidak |

---

## 20. Catatan untuk Demo Client

Fitur yang dapat ditunjukkan dalam demo awal:

1. Login dan beranda.
2. Profil santri dan ringkasan monitoring.
3. Hafalan Quran dan hafalan kitab.
4. Catatan kedisiplinan, kesehatan, dan perizinan.
5. Tagihan dan pilihan metode pembayaran.
6. Instruksi pembayaran dan status otomatis.
7. Dompet Santri dan alur top up/limit/riwayat.
8. Al-Quran Digital dengan pilihan qori dan full surah.
9. Jadwal sholat dan pengingat.
10. Cuaca dengan visual interaktif.
11. Tanya AI.
12. Berita dan notifikasi.
13. Forum/chat alumni jika akun alumni tersedia.

Tekankan bahwa aplikasi Android adalah kanal monitoring dan layanan untuk wali/alumni/kantin. Operasional input data pesantren tetap dilakukan oleh pengurus melalui sistem resmi pesantren/backend.
