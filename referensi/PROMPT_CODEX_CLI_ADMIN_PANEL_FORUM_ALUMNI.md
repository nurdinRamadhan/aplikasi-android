# Prompt Codex CLI - Implementasi Admin Panel Forum Alumni

Gunakan prompt ini di Codex CLI pada project admin panel:

`/home/arch-din1/Admin Panel/alhasanahAdmin`

## Peran

Anda adalah senior frontend engineer dan Supabase engineer. Tugas Anda adalah menyempurnakan implementasi sisi admin panel untuk fitur Forum Alumni yang sudah berjalan di aplikasi Android. Jangan mengerjakan fitur chat dulu. Fokus hanya pada admin panel untuk alumni dan forum.

## Instruksi Wajib

- Gunakan skill/MCP Supabase semaksimal mungkin untuk melihat struktur database nyata sebelum membuat asumsi.
- Gunakan MCP Supabase untuk:
  - `list_tables` pada schema `public`.
  - `execute_sql` untuk inspeksi kolom, constraint, enum/check constraint, RLS policy, dan foreign key.
  - `get_advisors` setelah perubahan yang menyentuh query/security.
  - `search_docs` bila perlu verifikasi pola Supabase/Reatime/RLS terbaru.
- Jangan mengubah schema database kecuali benar-benar diperlukan. Jika perlu perubahan database, jelaskan alasan, risiko, SQL yang akan dijalankan, dan pastikan tidak merusak flow Android yang sudah berjalan.
- Jangan menghapus atau merusak fitur admin panel existing.
- Ikuti style project admin panel yang sudah ada: Refine, Ant Design, ProTable, `supabaseClient`, resource routing existing.
- Hindari refactor besar yang tidak relevan.
- Semua halaman harus production-ready: loading state, empty state, error handling, confirmation modal untuk aksi berisiko, dan toast sukses/gagal.

## Konteks Project

Project admin panel berada di:

`/home/arch-din1/Admin Panel/alhasanahAdmin`

Referensi file copy/paste dari implementasi sebelumnya tersedia di project Android:

- `/home/arch-din1/Project Android/Alhasanah/alhasanahMedia/referensi/alumni/list.tsx`
- `/home/arch-din1/Project Android/Alhasanah/alhasanahMedia/referensi/alumni/forum-reports.tsx`

Target folder admin panel:

`/home/arch-din1/Admin Panel/alhasanahAdmin/src/pages/`

Kemungkinan file terkait yang perlu dicek:

- `src/pages/alumni/list.tsx`
- `src/pages/alumni/forum-reports.tsx`
- `src/App.tsx`
- `src/resources.tsx`
- `src/lazyPages.tsx`
- `src/types.ts`
- `src/utility/supabaseClient.ts`
- file access control / permission provider jika ada.

## Database Forum Alumni Yang Harus Diinspeksi

Jangan berasumsi. Verifikasi langsung via MCP Supabase, tetapi fitur forum Android saat ini memakai tabel berikut:

- `profiles`
- `alumni_data`
- `forum_threads`
- `forum_comments`
- `forum_reactions`
- `forum_attachments`
- `forum_reports`
- `notification_queue`

Kolom penting yang kemungkinan ada:

- `forum_threads`: `id`, `author_id`, `content`, `status`, `is_pinned`, `is_locked`, `comment_count`, `reaction_count`, `repost_of_thread_id`, `created_at`, `edited_at`, `deleted_at`
- `forum_comments`: `id`, `thread_id`, `author_id`, `content`, `status`, `reaction_count`, `created_at`, `edited_at`, `deleted_at`
- `forum_reports`: `id`, `reporter_id`, `thread_id`, `comment_id`, `reason`, `note`, `status`, `reviewed_by`, `reviewed_at`, `created_at`
- `alumni_data`: data profil alumni, preferensi privasi, preferensi notifikasi forum.
- `profiles`: role, status aktif, email, nama, foto.

Pastikan nama kolom nyata sesuai database sebelum dipakai.

## Tujuan Implementasi

Bangun admin panel Forum Alumni yang lengkap dan aman untuk operasional:

1. Manajemen Alumni
   - Tampilkan daftar alumni pending, aktif, dan semua.
   - Verifikasi/aktifkan alumni.
   - Nonaktifkan alumni bila diperlukan.
   - Edit data alumni dasar.
   - Lihat detail alumni: profil, kontak, profesi, domisili, bio, setting privasi, setting notifikasi.
   - Export Excel jika pola project sudah mendukung.
   - Avatar memakai signed URL dari bucket `alumni-avatars`.

2. Moderasi Forum Threads
   - Buat halaman admin untuk daftar posting forum.
   - Filter berdasarkan status: `published`, `pending_review`, `hidden`, `deleted`, dan semua.
   - Search berdasarkan isi posting, nama author, email author.
   - Tampilkan author, waktu, status, pinned, locked, jumlah komentar, jumlah reaction, jumlah attachment.
   - Aksi admin:
     - publish/restore posting.
     - hide posting.
     - pin/unpin.
     - lock/unlock komentar.
     - soft delete bila kolom `deleted_at` tersedia; jika tidak tersedia gunakan status sesuai constraint nyata.
   - Detail drawer menampilkan isi penuh posting, author, attachment image preview, status, dan metadata.

3. Moderasi Komentar
   - Tampilkan komentar forum dengan filter status dan search.
   - Detail komentar menampilkan isi komentar, author, thread asal, dan status.
   - Aksi admin:
     - publish/restore komentar.
     - hide komentar.
     - soft delete/status delete sesuai schema nyata.

4. Laporan Forum
   - Gunakan/sempurnakan halaman `forum-reports.tsx` dari referensi.
   - Filter status laporan: `open`, `reviewing`, `resolved`, `rejected`, semua.
   - Tampilkan konten yang dilaporkan, reporter, author konten, reason, note, created_at.
   - Aksi admin:
     - Tandai reviewing.
     - Resolve dan hide target.
     - Resolve tanpa hide jika laporan tidak valid.
     - Reject laporan.
     - Restore konten terkait bila perlu.
   - Saat laporan selesai, isi `reviewed_by` dengan user admin saat ini dan `reviewed_at` dengan timestamp.

5. Statistik Operasional
   - Card statistik ringkas:
     - alumni pending.
     - alumni aktif.
     - total posting published.
     - laporan open.
     - konten pending review.
   - Statistik harus tahan error; jangan membuat seluruh halaman crash jika salah satu query gagal.

6. Navigasi dan Resource
   - Tambahkan route/resource admin panel yang diperlukan.
   - Nama menu yang direkomendasikan:
     - `Alumni`
     - `Forum Alumni`
     - `Laporan Forum`
   - Gunakan icon Ant Design yang sesuai.
   - Pastikan lazy import/page registration sesuai pola project.

## RLS dan Keamanan

Sebelum implementasi aksi tulis, cek policy RLS nyata untuk:

- `alumni_data`
- `profiles`
- `forum_threads`
- `forum_comments`
- `forum_reports`
- `forum_attachments`

Pastikan admin panel hanya memakai akses yang memang diizinkan oleh policy. Jangan bypass memakai service role di frontend.

Jika aksi admin gagal karena RLS:

1. Jangan hardcode workaround di frontend.
2. Inspeksi policy.
3. Rancang policy admin berbasis role yang sudah ada di `profiles.role`.
4. Jika perlu SQL, gunakan MCP `execute_sql`, bukan migration, kecuali user secara eksplisit meminta migration.
5. Setelah perubahan, uji dengan query nyata dan jalankan advisor.

Role admin forum yang saat ini dipakai Android:

- `super_admin`
- `kesantrian`
- `rois`

Gunakan role ini untuk akses moderasi forum, kecuali database nyata menunjukkan model role berbeda.

## Realtime Admin Panel

Tambahkan Realtime hanya bila tidak membuat kompleksitas berlebihan:

- Subscribe perubahan `forum_reports` untuk laporan baru.
- Subscribe `forum_threads` dan `forum_comments` untuk update status.
- Jika Realtime belum siap di admin panel, minimal sediakan tombol refresh yang jelas dan loading state baik.

## UX Admin Panel

Gunakan gaya profesional operasional, bukan landing page.

Wajib ada:

- Table padat dan mudah discan.
- Tag status berwarna konsisten.
- Drawer untuk detail.
- Modal konfirmasi untuk aksi hide/delete/restore/resolve.
- Message toast untuk hasil aksi.
- Empty state.
- Loading state.
- Error message yang actionable.

Jangan:

- Membuat UI marketing.
- Mengubah layout global admin panel secara besar-besaran.
- Membuat warna hardcoded berlebihan bila project punya token/theme.
- Menghapus resource existing.

## Langkah Kerja Yang Harus Dilakukan Codex CLI

1. Baca struktur project admin panel.
2. Baca file referensi:
   - `referensi/alumni/list.tsx`
   - `referensi/alumni/forum-reports.tsx`
3. Gunakan MCP Supabase untuk inspeksi schema nyata.
4. Cek types existing dan sesuaikan bila perlu.
5. Implementasikan/sempurnakan halaman:
   - alumni list.
   - forum threads moderation.
   - forum comments moderation.
   - forum reports moderation.
6. Registrasikan routes/resources/lazy pages sesuai pola project.
7. Pastikan query Supabase hanya memakai kolom yang benar-benar ada.
8. Jalankan typecheck/build/lint yang tersedia.
9. Jika build gagal, perbaiki sampai berhasil.
10. Berikan ringkasan file yang diubah, SQL yang dijalankan jika ada, dan hasil verifikasi.

## Kriteria Selesai

Implementasi dianggap selesai jika:

- Admin bisa melihat dan memverifikasi alumni.
- Admin bisa melihat posting forum.
- Admin bisa hide/restore, pin/unpin, lock/unlock posting.
- Admin bisa melihat komentar forum dan memoderasi komentar.
- Admin bisa melihat laporan forum dan menyelesaikan laporan.
- Menu/route admin panel tampil dan bisa diakses.
- Build/typecheck admin panel sukses.
- Tidak ada perubahan yang memutus fitur Android Forum Alumni.

## Catatan Penting

Fitur chat realtime belum dikerjakan pada tahap ini. Jangan membuat tabel chat, halaman chat, DM, group chat, atau UI messenger. Fokus hanya pada admin panel untuk Forum Alumni dan moderasi fitur yang sudah ada.
