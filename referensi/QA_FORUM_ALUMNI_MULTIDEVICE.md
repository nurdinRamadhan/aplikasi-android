# QA Multi-Device Forum Alumni

Tanggal dibuat: 2026-05-15

Dokumen ini dipakai untuk menguji interaksi Forum Alumni pada 2 device atau 2 emulator. Fokus utama: realtime feed, komentar, love/reaction, laporan, moderasi, notifikasi, dan deep link ke thread.

## Prasyarat

- Device A login sebagai alumni aktif.
- Device B login sebagai alumni aktif yang berbeda.
- Device C opsional login sebagai admin forum (`super_admin`, `kesantrian`, atau `rois`).
- Semua akun punya FCM token terbaru setelah login.
- Koneksi internet stabil.
- Supabase Realtime aktif untuk:
  - `forum_threads`
  - `forum_comments`
  - `forum_reactions`
  - `forum_reports`
- Edge Function `push-notifications` aktif dan menerima item dari `notification_queue`.

## Skenario 1 - Load Awal Forum

| Langkah | Device | Ekspektasi |
|---|---|---|
| Buka `Forum Alumni` dari app bar alumni | A | Feed tampil tanpa delay panjang. Target ideal < 2 detik pada koneksi normal. |
| Buka `Forum Alumni` | B | Feed sama dengan A, urutan pin dan waktu konsisten. |
| Pull to refresh | A/B | Tidak ada duplikasi post, komentar, atau reaction. |

Catatan hasil:

- Device A:
- Device B:
- Masalah:

## Skenario 2 - Post Baru Realtime

| Langkah | Device | Ekspektasi |
|---|---|---|
| Buat posting teks baru | A | Composer tertutup, post muncul di feed A. |
| Tunggu tanpa refresh manual | B | Post A muncul otomatis lewat realtime. Target ideal < 2 detik setelah post berhasil. |
| Klik post baru | B | Detail thread terbuka dan konten sesuai. |

Catatan hasil:

- Waktu post muncul di B:
- Masalah:

## Skenario 3 - Post Gambar

| Langkah | Device | Ekspektasi |
|---|---|---|
| Buat posting dengan gambar | A | Upload berhasil, post muncul dengan preview gambar. |
| Tunggu realtime | B | Post gambar muncul otomatis. |
| Tap gambar | B | Preview gambar terbuka, tidak blank, tidak crash. |

Catatan hasil:

- Format gambar:
- Ukuran file:
- Masalah:

## Skenario 4 - Komentar Realtime

| Langkah | Device | Ekspektasi |
|---|---|---|
| Buka detail thread yang sama | A dan B | Keduanya berada di thread yang sama. |
| Kirim komentar | B | Komentar langsung muncul optimistis di B. |
| Tunggu tanpa refresh | A | Komentar B muncul otomatis lewat realtime. |
| Kirim komentar balasan | A | Komentar muncul di A dan B tanpa duplikasi. |

Catatan hasil:

- Waktu komentar muncul di device lain:
- Ada duplikasi/tidak:
- Masalah:

## Skenario 5 - Love / Reaction Realtime

| Langkah | Device | Ekspektasi |
|---|---|---|
| Tap love pada post | B | Icon dan count berubah langsung di B. |
| Amati feed/detail | A | Count berubah otomatis lewat realtime. |
| Tap love lagi untuk unlike | B | Count turun dan sinkron di A. |
| Love komentar | A | Reaction komentar berubah langsung dan tersinkron di B. |

Catatan hasil:

- Waktu count sinkron:
- Count negatif/tidak:
- Masalah:

## Skenario 6 - Edit dan Delete

| Langkah | Device | Ekspektasi |
|---|---|---|
| Edit post milik A | A | Muncul pesan sukses, konten update. |
| Amati post | B | Konten berubah otomatis atau setelah realtime refresh singkat. |
| Hapus post uji | A | Post hilang di A. |
| Amati feed | B | Post hilang tanpa refresh manual. |

Catatan hasil:

- Waktu sinkron:
- Masalah:

## Skenario 7 - Report dan Moderasi

| Langkah | Device | Ekspektasi |
|---|---|---|
| Laporkan post A | B | Muncul pesan laporan terkirim. |
| Cek feed pengguna biasa | A/B | Post yang dilaporkan bisa masuk status review sesuai policy database. |
| Buka sebagai admin forum | C | Admin bisa melihat menu moderasi. |
| Admin hide post | C | Post tidak tampil untuk pengguna biasa. |
| Admin restore post | C | Post kembali tampil. |
| Admin lock komentar | C | User biasa tidak bisa menambah komentar baru pada thread terkunci. |
| Admin pin post | C | Post naik ke posisi pinned. |

Catatan hasil:

- Admin role yang dipakai:
- Status post setelah report:
- Masalah:

## Skenario 8 - Notifikasi Forum

| Langkah | Device | Ekspektasi |
|---|---|---|
| B komentar di post A | B | A menerima notifikasi forum comment. |
| Tap notifikasi push | A | App terbuka ke Forum Alumni dan langsung membuka thread terkait. |
| B love post A | B | A menerima notifikasi forum reaction jika setting aktif. |
| Buka tab `Notifikasi` di app bar alumni | A | Notifikasi forum berlabel `Forum Alumni`. |
| Tap notifikasi in-app | A | Status menjadi read dan thread terkait terbuka. |

Catatan hasil:

- Push diterima saat app foreground:
- Push diterima saat background:
- Push diterima saat killed state:
- Deep link benar/tidak:
- Masalah:

## Skenario 9 - Repost

| Langkah | Device | Ekspektasi |
|---|---|---|
| Repost thread dari profil alumni | A | Repost dibuat dengan label `Posting ulang`. |
| Amati feed | B | Repost muncul realtime. |
| Buka profil A | B | Repost muncul di daftar posting A. |

Catatan hasil:

- Masalah:

## Skenario 10 - Navigasi App Bar Alumni

| Langkah | Device | Ekspektasi |
|---|---|---|
| Tap `Threads` | A | Masuk feed forum. |
| Tap `Chat` | A | Masuk halaman chat placeholder/fitur chat. |
| Tap `Notifikasi` | A | Masuk halaman notifikasi. |
| Tap `Profil` | A | Masuk profil alumni. |
| Pindah bolak-balik antar menu | A | Tidak terasa loading berat; state feed tidak reset berlebihan. |

Catatan hasil:

- Menu yang lambat:
- Masalah:

## Skenario 11 - Chat Realtime Alumni

| Langkah | Device | Ekspektasi |
|---|---|---|
| Buka tab `Chat` | A | Daftar chat tampil, atau empty state dengan tombol chat baru. |
| Mulai chat baru ke alumni B | A | Room chat langsung terbuka. |
| Kirim pesan teks | A | Pesan tampil optimistis dengan status `mengirim`, lalu berubah menjadi `terkirim`. |
| Amati Device B tanpa refresh | B | Percakapan muncul realtime, unread badge bertambah. |
| Buka percakapan | B | Pesan A tampil, unread hilang setelah dibuka. |
| Ketik balasan tanpa mengirim | B | Device A menampilkan indikator `sedang mengetik`. |
| Kirim balasan | B | Device A menerima pesan realtime. |
| Amati pesan A | A | Status berubah menjadi `dibaca` setelah B membuka room. |

Catatan hasil:

- Waktu pesan muncul di device lain:
- Typing indicator muncul/tidak:
- Read status akurat/tidak:
- Masalah:

## Skenario 12 - Notifikasi dan Deep Link Chat

| Langkah | Device | Ekspektasi |
|---|---|---|
| Tutup/background app di Device B | B | App tidak aktif di foreground. |
| Kirim pesan dari A ke B | A | B menerima push notification `Chat Alumni`. |
| Tap push notification | B | App membuka tab Chat dan langsung masuk room conversation terkait. |
| Buka notifikasi in-app | B | Notifikasi chat berlabel `Chat Alumni`. |
| Tap notifikasi in-app | B | Status notifikasi menjadi read dan room chat terbuka. |

Catatan hasil:

- Push foreground:
- Push background:
- Push killed state:
- Deep link conversation benar/tidak:
- Masalah:

## Skenario 13 - Mute, Archive, Report, Block Chat

| Langkah | Device | Ekspektasi |
|---|---|---|
| Mute chat dari room | B | Pesan baru dari A tetap masuk realtime jika room terbuka, tetapi push tidak dikirim saat B tidak membuka app. |
| Unmute chat | B | Push pesan berikutnya kembali diterima. |
| Archive chat | B | Chat hilang dari daftar B, tidak menghapus chat di A. |
| Laporkan pesan dari lawan bicara | B | Muncul pesan laporan terkirim, row masuk `chat_message_reports`. |
| Blokir alumni A | B | Room tertutup, A tidak bisa mengirim pesan baru ke B. |

Catatan hasil:

- Mute mencegah push:
- Archive hanya untuk user sendiri:
- Report masuk database:
- Block efektif:
- Masalah:

## Skenario 14 - Reliability dan Rate Limit Chat

| Langkah | Device | Ekspektasi |
|---|---|---|
| Kirim beberapa pesan sangat cepat | A | Sistem menahan spam/rate limit; UI menampilkan error yang bisa retry. |
| Matikan koneksi lalu kirim pesan | A | Bubble pesan tetap muncul dengan status `belum terkirim` dan tombol `Coba lagi`. |
| Tutup paksa app, buka lagi di room yang sama | A | Pesan gagal masih tampil karena tersimpan di outbox lokal. |
| Nyalakan koneksi lalu retry dari bubble | A | Pesan berhasil terkirim dan item outbox hilang tanpa duplikasi. |
| Hapus pesan sendiri | A | Pesan hilang/berstatus deleted untuk peserta chat. |

Catatan hasil:

- Rate limit muncul:
- Outbox tahan restart:
- Retry dari bubble berhasil:
- Duplikasi ada/tidak:
- Masalah:

## Skenario 15 - Pagination dan Presence Chat

| Langkah | Device | Ekspektasi |
|---|---|---|
| Siapkan percakapan dengan lebih dari 50 pesan | A/B | Room hanya memuat batch terbaru terlebih dahulu. |
| Tap `Muat pesan lama` | A/B | Pesan lama muncul di atas tanpa menggeser user kembali ke bawah secara paksa. |
| Buka chat di kedua device | A/B | Subtitle lawan bicara menampilkan `online`. |
| Tutup app di salah satu device | A/B | Dalam siklus heartbeat berikutnya status berubah menjadi `terakhir dilihat ...`. |
| Salah satu user mengetik | A/B | Subtitle berubah menjadi `sedang mengetik...`, lalu kembali ke online/last seen setelah idle. |

Catatan hasil:

- Pagination 50 pesan:
- Scroll tetap stabil:
- Online/last seen akurat:
- Typing kembali normal:
- Masalah:

## Checklist Bug Kritis

- [ ] Post tidak muncul realtime di device lain.
- [ ] Komentar muncul duplikat.
- [ ] Love count salah atau negatif.
- [ ] Notifikasi push tidak membawa `thread_id`.
- [ ] Tap notifikasi membuka forum tetapi tidak membuka thread.
- [ ] Admin tidak bisa masuk forum.
- [ ] Admin tidak melihat menu moderasi.
- [ ] User biasa bisa melihat konten hidden.
- [ ] User biasa bisa komentar di thread locked.
- [ ] Loading pindah halaman > 4 detik secara konsisten.
- [ ] Chat tidak muncul realtime di device lain.
- [ ] Typing indicator tidak tampil atau tidak hilang.
- [ ] Read status salah.
- [ ] Push chat tidak membawa `conversation_id`.
- [ ] Tap notifikasi chat tidak membuka room yang benar.
- [ ] Mute chat masih mengirim push.
- [ ] Archive menghapus chat untuk peserta lain.
- [ ] Block user tidak mencegah pesan baru.
- [ ] Report chat tidak masuk `chat_message_reports`.
- [ ] Retry pesan gagal membuat duplikasi.
- [ ] Outbox pesan gagal hilang setelah restart app.
- [ ] Pagination chat menggeser layar ke bawah saat memuat pesan lama.
- [ ] Presence selalu online meski user sudah keluar.
- [ ] Last seen tidak berubah setelah app ditutup.

## Hasil Akhir

Status QA:

- [ ] Lulus
- [ ] Lulus dengan catatan
- [ ] Gagal

Catatan ringkas:

-
