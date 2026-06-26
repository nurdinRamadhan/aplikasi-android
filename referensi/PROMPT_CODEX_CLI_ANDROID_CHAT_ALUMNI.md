# Prompt Codex CLI - Implementasi Chat Realtime Alumni Android

Gunakan prompt ini di Codex CLI pada project Android:

`/home/arch-din1/Project Android/Alhasanah/alhasanahMedia`

## Peran

Anda adalah senior Android engineer, Supabase engineer, dan product engineer. Tugas Anda adalah menyempurnakan fitur Chat Realtime Alumni di aplikasi Android agar terasa seperti aplikasi chat premium: cepat, realtime, stabil, aman, dan siap produksi.

## Instruksi Wajib

- Gunakan skill/MCP Supabase semaksimal mungkin sebelum membuat asumsi database.
- Gunakan MCP Supabase untuk:
  - `list_tables` pada schema `public`.
  - `execute_sql` untuk inspeksi kolom, constraint, trigger, index, RLS policy, realtime publication, dan foreign key.
  - `get_advisors` setelah perubahan database/security/performance.
  - `search_docs` bila perlu verifikasi pola Supabase terbaru.
- Jangan memakai migration kecuali user eksplisit meminta. Database masih development, tetapi perubahan tetap harus aman dan tidak merusak flow lain.
- Jangan mengubah UI/UX global aplikasi di luar scope chat alumni.
- Jangan merusak Forum Alumni, Profile Alumni, Register Alumni, Notification, dan Auth flow yang sudah berjalan.
- Ikuti style Kotlin/Jetpack Compose/Koin/Supabase yang sudah ada di project.
- Jalankan `./gradlew :app:compileDebugKotlin` setelah perubahan. Jika gagal, perbaiki sampai sukses.

## Konteks Project

Project Android berada di:

`/home/arch-din1/Project Android/Alhasanah/alhasanahMedia`

File utama fitur chat yang perlu dicek:

- `app/src/main/java/com/alhasanah/alhasanahmedia/ui/alumni/AlumniChatScreen.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/ui/alumni/AlumniChatViewModel.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/ChatRepository.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/data/repository/ChatOutboxStore.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/data/model/AlumniForumModels.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/di/AppModule.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/di/ViewModelModule.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/MainActivity.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/fcm/MyFirebaseMessagingService.kt`
- `app/src/main/java/com/alhasanah/alhasanahmedia/ui/notifikasi/NotificationScreen.kt`

File QA yang harus diperbarui bila ada perubahan behavior:

- `referensi/QA_FORUM_ALUMNI_MULTIDEVICE.md`

## Database Chat Yang Harus Diinspeksi

Jangan berasumsi. Verifikasi langsung dengan MCP Supabase. Fitur chat saat ini memakai tabel berikut:

- `profiles`
- `alumni_data`
- `chat_conversations`
- `chat_participants`
- `chat_messages`
- `chat_message_reports`
- `chat_blocks`
- `chat_user_presence`
- `notification_queue`
- `user_devices`

Kolom yang kemungkinan ada:

- `chat_conversations`: `id`, `type`, `title`, `created_by`, `created_at`, `updated_at`, `last_message_at`, `last_message_preview`, `last_message_sender_id`
- `chat_participants`: `conversation_id`, `user_id`, `role`, `joined_at`, `last_read_at`, `muted_until`, `archived_at`
- `chat_messages`: `id`, `conversation_id`, `sender_id`, `content`, `message_type`, `status`, `reply_to_message_id`, `created_at`, `edited_at`, `deleted_at`
- `chat_message_reports`: `id`, `message_id`, `conversation_id`, `reporter_id`, `reason`, `note`, `status`, `reviewed_by`, `reviewed_at`, `created_at`
- `chat_blocks`: `blocker_id`, `blocked_id`, `created_at`
- `chat_user_presence`: `user_id`, `is_online`, `last_seen_at`, `updated_at`

Pastikan nama kolom nyata sesuai database sebelum dipakai.

## Status Fitur Saat Ini

Fitur chat sudah memiliki pondasi:

- Daftar percakapan alumni.
- Membuat direct chat antar alumni.
- Room chat realtime berbasis Supabase Realtime.
- Typing indicator via broadcast channel.
- Push notification FCM untuk pesan chat.
- Tap push/in-app notification membuka room chat berdasarkan `conversation_id`.
- Moderasi dasar: report message, block user, archive, mute, delete own message.
- Pagination pesan dengan tombol `Muat pesan lama`.
- Outbox lokal untuk pesan gagal kirim yang tahan restart app.
- Presence online/last seen berbasis `chat_user_presence`.

Tugas Anda adalah inspeksi ulang dan menyempurnakan bagian yang belum optimal, bukan menulis ulang dari nol.

## Tujuan Implementasi

### 1. Stabilitas Realtime

- Pesan masuk harus muncul cepat di device lawan tanpa refresh manual.
- Pesan sendiri harus optimistic dan tidak duplikat ketika event realtime masuk.
- Typing indicator harus muncul cepat dan hilang setelah idle.
- Presence online/last seen harus masuk akal:
  - online saat user aktif.
  - fallback ke last seen jika heartbeat stale.
  - tidak membuat user terlihat online selamanya.

### 2. Reliability Pengiriman Pesan

- Saat online: pesan terkirim cepat dan status berubah wajar.
- Saat offline/gagal: bubble tetap muncul dengan status `belum terkirim`.
- Outbox lokal harus bertahan setelah app ditutup/dibuka ulang.
- Tombol `Coba lagi` harus mengirim ulang tanpa duplikasi.
- Rate limit dari database harus ditampilkan sebagai feedback yang bisa dipahami user.

### 3. Pagination dan Performa

- Room chat tidak boleh memuat semua pesan sekaligus.
- Gunakan batch terbaru, misalnya 50 pesan.
- `Muat pesan lama` mengambil pesan sebelum timestamp pesan server tertua.
- Saat memuat pesan lama, posisi scroll jangan dipaksa turun ke pesan terbaru.
- Query harus memakai index yang memadai.

### 4. UX Premium Chat

- Pertahankan UI premium yang sudah ada.
- Jangan ubah desain besar tanpa kebutuhan jelas.
- Perbaiki hanya state/interaksi yang terasa lambat atau tidak konsisten.
- Bubble harus punya state:
  - mengirim.
  - terkirim.
  - dibaca.
  - belum terkirim.
- Top bar room harus menampilkan:
  - nama alumni.
  - typing indicator.
  - online/last seen.
  - menu mute/archive/block.
- Empty state dan loading state harus rapi.

### 5. Notifikasi

- Pastikan FCM chat memakai payload minimal:
  - `type = alumni_chat_message`
  - `conversation_id`
  - `source = chat_messages`
  - `target_user_id`
- Tap notifikasi harus membuka `Screen.AlumniChat.createRoute(conversationId)`.
- Mute chat harus menghentikan push untuk conversation tersebut.
- Block user harus mencegah pengiriman pesan baru ke user yang memblokir.

### 6. Keamanan dan RLS

Inspeksi policy RLS nyata untuk:

- `chat_conversations`
- `chat_participants`
- `chat_messages`
- `chat_message_reports`
- `chat_blocks`
- `chat_user_presence`

Pastikan:

- Hanya alumni aktif yang bisa memakai chat.
- User hanya bisa membaca conversation yang ia ikuti.
- User hanya bisa mengirim sebagai dirinya sendiri.
- User tidak bisa mengirim ke orang yang memblokirnya.
- User hanya bisa update presence miliknya sendiri.
- User hanya bisa delete/soft-delete pesan miliknya sendiri.
- Report message hanya bisa dilakukan peserta conversation.

Jika ada celah RLS:

1. Jelaskan risiko.
2. Buat SQL yang scoped.
3. Eksekusi via MCP `execute_sql`.
4. Jalankan advisor setelahnya.

## Langkah Kerja Yang Harus Dilakukan Codex CLI

1. Baca struktur project Android dan file chat terkait.
2. Baca QA multidevice di `referensi/QA_FORUM_ALUMNI_MULTIDEVICE.md`.
3. Gunakan MCP Supabase untuk inspeksi schema dan policy chat nyata.
4. Jalankan advisor Supabase security/performance.
5. Identifikasi gap antara code Android dan schema Supabase.
6. Implementasikan perbaikan Android secara bertahap:
   - repository/query.
   - ViewModel state.
   - outbox/retry.
   - UI state chat.
   - notification routing bila perlu.
7. Jika perlu perbaikan database chat, gunakan MCP `execute_sql` dengan SQL idempotent.
8. Perbarui file QA jika behavior berubah atau ada skenario baru.
9. Jalankan `./gradlew :app:compileDebugKotlin`.
10. Berikan ringkasan file yang diubah, SQL yang dijalankan, hasil compile, dan risiko sisa.

## Batasan Penting

- Jangan mengerjakan admin panel pada prompt ini.
- Jangan menambahkan fitur kirim gambar/file kecuali user meminta eksplisit.
- Jangan mengubah desain besar Forum Alumni.
- Jangan mengganti sistem auth global.
- Jangan memakai service role di aplikasi Android.
- Jangan menghapus RLS.
- Jangan melakukan reset database.

## Kriteria Selesai

Implementasi dianggap selesai jika:

- Chat list dan chat room terbuka tanpa crash.
- Pesan terkirim realtime antar device.
- Typing indicator bekerja.
- Online/last seen bekerja dengan fallback stale.
- Pagination pesan lama bekerja.
- Outbox lokal bertahan setelah restart app.
- Retry pesan gagal tidak membuat duplikasi.
- Push chat membuka room yang benar.
- Mute, archive, report, block, dan delete own message tetap bekerja.
- `./gradlew :app:compileDebugKotlin` sukses.
- QA multidevice diperbarui sesuai perubahan.

## Output Akhir Yang Harus Diberikan Codex CLI

Berikan jawaban akhir dalam bahasa Indonesia berisi:

- Ringkasan perubahan.
- File yang diubah.
- SQL/Supabase action yang dijalankan.
- Hasil compile/test.
- Catatan risiko atau hal yang perlu diuji manual di multi-device.
