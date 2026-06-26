# Prompt Codex CLI - Stabilisasi Forum Alumni, Chat, Realtime, Navigasi, dan Cache Android

Gunakan prompt ini di Codex CLI pada project Android:

`/home/arch-din1/Project Android/Alhasanah/alhasanahMedia`

## Peran

Anda adalah senior Android engineer, Supabase engineer, dan reliability engineer. Tugas Anda adalah melanjutkan stabilisasi fitur Forum Alumni dan Chat Alumni agar aplikasi terasa ringan, instant, realtime, dan aman untuk produksi.

## Instruksi Wajib

- Gunakan skill/MCP Supabase untuk semua hal yang menyentuh Supabase, RLS, Realtime, Auth, Edge Function, Storage, atau schema.
- Jangan membuat migration kecuali user eksplisit meminta. Gunakan MCP `execute_sql` untuk development database.
- Jangan mengubah UI/UX premium yang sudah dibangun, kecuali perubahan kecil untuk state/error/loading.
- Jangan merusak flow yang sudah berjalan: Auth, Forum Alumni, Chat Alumni, Profile Alumni, Register Alumni, Notification, dan bottom navigation.
- Saat user mengirim logcat, prioritaskan stacktrace `FATAL EXCEPTION` dan patch sumber crash langsung.
- Setelah perubahan Android, jalankan:

```bash
./gradlew :app:compileDebugKotlin
```

## Konteks Perubahan Terbaru

Fitur Forum Alumni dan Chat Alumni sudah mengalami banyak perubahan. Codex CLI harus memahami kondisi terbaru sebelum patch:

### Forum Alumni

- Forum alumni memakai screen:
  - `ui/alumni/ForumAlumniScreen.kt`
  - `ui/alumni/ForumAlumniViewModel.kt`
  - `data/repository/ForumRepository.kt`
  - `data/repository/AlumniRepository.kt`
  - `data/model/AlumniForumModels.kt`
- Feed sudah mendukung:
  - post/thread.
  - komentar.
  - love/reaction.
  - upload gambar.
  - report/moderasi.
  - rekomendasi alumni untuk follow.
  - optimistic UI untuk komentar/love/follow.
- Realtime forum menggunakan Supabase Realtime untuk:
  - `forum_threads`
  - `forum_comments`
  - `forum_reactions`
  - `forum_reports`
  - `alumni_follows`
- Bug penting yang sudah diperbaiki:
  - Crash `IllegalStateException: You cannot call postgresChangeFlow after joining the channel`.
  - Penyebab: channel Realtime memakai nama tetap sehingga channel lama yang sudah `joined` bisa dipakai ulang.
  - Fix: channel forum dibuat unik:

```kotlin
supabaseClient.channel("forum-alumni-live:${access.userId}:${System.identityHashCode(this)}")
```

Jangan kembalikan channel Realtime ke nama statis.

### Follow Alumni

Database sudah memiliki tabel:

- `public.alumni_follows`

Struktur utama:

- `follower_id uuid`
- `following_id uuid`
- `created_at timestamptz`
- PK `(follower_id, following_id)`
- check `follower_id <> following_id`
- FK ke `profiles(id)` dengan cascade delete

RLS follow terbaru:

- SELECT: alumni aktif bisa membaca follow graph.
- INSERT: user harus `auth.uid()`, bukan diri sendiri, alumni aktif, dan target ada di `alumni_data`.
- DELETE: user hanya bisa unfollow miliknya sendiri.

Policy insert terakhir sengaja memakai target `alumni_data`, bukan wajib `profiles.role='alumni'`, karena data development bisa bervariasi.

Jika error RLS follow muncul lagi:

1. Cek `auth.uid()` user yang login.
2. Cek user tersebut ada di `profiles` dan `alumni_data`.
3. Cek `profiles.role = 'alumni'` dan `profiles.is_active = true` untuk follower.
4. Cek target ada di `alumni_data`.
5. Inspeksi `pg_policies` untuk `public.alumni_follows`.

### Profile Alumni

- Profile menampilkan:
  - post count.
  - comment count.
  - reaction count.
  - follower count.
  - following count.
  - tombol `Ikuti/Mengikuti`.
  - tombol `Pesan`.
- Tombol `Pesan` membuka chat via:

```kotlin
Screen.AlumniChat.createDirectRoute(alumniId)
```

### Chat Alumni

- Chat memakai:
  - `ui/alumni/AlumniChatScreen.kt`
  - `ui/alumni/AlumniChatViewModel.kt`
  - `data/repository/ChatRepository.kt`
  - `data/repository/ChatOutboxStore.kt`
- Route chat mendukung:
  - `conversationId`
  - `targetUserId`

Route:

```kotlin
object AlumniChat : Screen("alumni_chat?conversationId={conversationId}&targetUserId={targetUserId}") {
    const val baseRoute = "alumni_chat"
    fun createRoute(conversationId: String? = null) =
        conversationId?.let { "alumni_chat?conversationId=$it" } ?: baseRoute
    fun createDirectRoute(targetUserId: String) = "alumni_chat?targetUserId=$targetUserId"
}
```

- `initialTargetUserId` di `AlumniChatScreen` akan memanggil `viewModel.openInitialDirectChat(...)`.
- Direct chat akan get-or-create conversation lalu membuka room.
- Realtime chat menggunakan channel unik:

```kotlin
supabaseClient.channel("alumni-chat-db:${currentAccess.userId}:${System.identityHashCode(this)}")
```

Jangan kembalikan ke nama statis.

### Navigasi dan Bottom App Bar

- Forum alumni memiliki bottom app bar khusus:
  - Threads
  - Chat
  - Notifikasi
  - Profil
- Crash saat klik cepat sudah dikurangi dengan:
  - `launchSingleTop`
  - `restoreState`
  - `popUpTo(graph.findStartDestination().id)`
  - guard route aktif.
  - debounce 450 ms pada `BottomAppBarButton`.
- Jangan menghapus debounce.
- Jika masih crash saat klik cepat, cek:
  - apakah ada `navigate()` dipanggil saat route belum siap.
  - apakah route optional query param valid.
  - apakah ViewModel melakukan crash dari coroutine main.

### Auth, Login, dan Splash

- `AuthRepositoryImpl.getAuthState()` memakai `distinctUntilChanged()`.
- Login sukses memakai fallback:
  - coba `popBackStack()`.
  - jika gagal, navigate ke `Home`.
- Splash navigation memakai `navigate(Home)` dengan `popUpTo(Splash)` dan `launchSingleTop`.

Jangan kembali memakai pola `popBackStack()` lalu `navigate()` tanpa guard.

### Cache Saat Ini

Sudah ada cache ringan in-memory di `AlumniRepositoryImpl`:

- `AlumniAccess` cache per user.
- `AlumniDirectoryItem` cache 5 menit.

Ini hanya optimasi sementara, bukan cache produksi penuh.

## Target Berikutnya

Setelah crash benar-benar stabil, lanjutkan membuat cache lokal production-ready:

### 1. DataStore

Gunakan DataStore untuk data kecil:

- last authenticated user id.
- cached alumni access summary.
- sync timestamp.
- preferensi tampilan/loading.

### 2. Room

Gunakan Room untuk data yang harus tampil instant:

- forum threads terbaru.
- forum attachments ringkas.
- forum comments untuk thread yang pernah dibuka.
- alumni recommendations.
- alumni directory/profile summary.
- chat conversations.
- chat messages per conversation.
- chat participants/presence ringkas.

### 3. Pola Stale-While-Revalidate

UI harus:

1. Tampilkan cache lokal segera.
2. Fetch Supabase di background.
3. Update Room/DataStore.
4. UI ikut update dari Flow lokal.
5. Realtime menjadi invalidation/update layer, bukan satu-satunya sumber data.

### 4. Offline/Retry

- Chat sudah memiliki outbox untuk pesan gagal.
- Pertahankan dan integrasikan dengan Room jika dibuat.
- Jangan membuat pesan duplikat saat server event masuk.

## Supabase Yang Harus Dicek

Sebelum perubahan Supabase, inspeksi:

- `profiles`
- `alumni_data`
- `alumni_follows`
- `forum_threads`
- `forum_comments`
- `forum_reactions`
- `forum_attachments`
- `forum_reports`
- `chat_conversations`
- `chat_participants`
- `chat_messages`
- `chat_blocks`
- `chat_user_presence`
- `notification_queue`
- `user_devices`

Gunakan:

- `list_tables`
- `execute_sql`
- `get_advisors`
- `search_docs` jika perlu

## Checklist Saat Ada Logcat Baru

Saat user mengirim `FATAL EXCEPTION`:

1. Identifikasi exception type.
2. Ambil file dan line number pertama di package `com.alhasanah.alhasanahmedia`.
3. Baca area kode sekitar line tersebut.
4. Patch minimal di sumber error.
5. Cek apakah pola error juga ada di ViewModel/screen sejenis.
6. Compile.
7. Jelaskan penyebab dan file yang diubah.

Contoh error yang sudah pernah terjadi:

```text
java.lang.IllegalStateException: You cannot call postgresChangeFlow after joining the channel
at ForumAlumniViewModel.startRealtime(...)
```

Solusi yang sudah diterapkan: channel Realtime unik per instance ViewModel.

## Kriteria Selesai Stabilisasi

Stabilisasi dianggap selesai jika:

- Login setelah register tidak force close.
- Forum alumni terbuka tanpa crash.
- Chat alumni terbuka tanpa delay besar.
- Bottom app bar alumni tidak crash saat diklik cepat.
- Follow alumni tidak error RLS.
- Realtime forum/chat tetap berjalan.
- Loading “memeriksa akses” tidak muncul berulang pada user yang sama.
- `./gradlew :app:compileDebugKotlin` sukses.

## Output Akhir Yang Harus Diberikan Codex CLI

Jawab dalam bahasa Indonesia berisi:

- Penyebab masalah.
- Perubahan yang dibuat.
- File yang diubah.
- SQL/Supabase action yang dijalankan.
- Hasil compile.
- Risiko sisa dan hal yang perlu diuji manual.
