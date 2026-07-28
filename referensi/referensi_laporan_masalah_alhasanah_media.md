# Referensi Implementasi Fitur Laporan Masalah
## Sinkron antara Database, Aplikasi Kotlin, dan Admin Panel

Dokumen ini menjadi panduan implementasi fitur **Laporan Masalah / Bantuan & Masukan** agar seluruh alur saling terhubung dengan rapi:

- dari **aplikasi Android Kotlin**,
- masuk ke **database Supabase**,
- lalu muncul di **admin panel**,
- dan dapat diproses sampai status masalah selesai.

Tujuan utama fitur ini adalah membuat proses debug, support, dan perbaikan bug menjadi lebih terstruktur, cepat, dan mudah ditelusuri ke depan.

---

## 1. Tujuan Fitur

Fitur ini dibuat untuk:

- menerima laporan bug dari wali santri atau pengguna aplikasi,
- menangkap konteks perangkat secara otomatis,
- menyimpan laporan secara rapi di database,
- menampilkan laporan di admin panel,
- memberi status penanganan yang jelas,
- mempermudah debugging di masa depan,
- menjadi kanal resmi antara pengguna dan pengelola aplikasi.

Fitur ini bukan hanya “form keluhan”, tetapi **sistem tiket internal** yang ringan dan konsisten.

---

## 2. Prinsip Desain yang Disarankan

Agar sistem enak dipakai dan mudah dirawat, beberapa prinsip berikut sebaiknya dijaga:

1. **Satu sumber data yang jelas**  
   Semua laporan disimpan di satu tabel utama.

2. **Status lebih baik daripada boolean**  
   Jangan hanya pakai `is_fixed`. Gunakan `status` agar proses bisa dilacak.

3. **Metadata perangkat otomatis**  
   Pengguna cukup menulis masalahnya. Detail teknis dikirim otomatis oleh aplikasi.

4. **Admin panel sebagai pusat tindak lanjut**  
   Admin tidak hanya melihat laporan, tetapi juga mengubah status dan memberi catatan.

5. **Riwayat harus bisa diaudit**  
   Setiap perubahan status sebaiknya terekam.

---

## 3. Model Data yang Disarankan

### 3.1 Tabel utama: `laporan_masalah`

Contoh struktur yang disarankan:

```sql
create table public.laporan_masalah (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  user_id uuid null references auth.users(id),
  nama_pengguna text null,
  nis text null,

  judul text not null,
  deskripsi text not null,
  kategori text not null default 'BUG',

  prioritas text not null default 'MEDIUM',
  status text not null default 'OPEN',

  app_version text null,
  android_version text null,
  device_brand text null,
  device_model text null,
  device_manufacturer text null,
  device_sdk integer null,
  locale text null,
  timezone text null,

  screenshot_url text null,
  attachment_paths jsonb not null default '[]'::jsonb,

  admin_note text null,
  fixed_at timestamptz null,
  fixed_by uuid null references auth.users(id),

  source text not null default 'android',
  is_public boolean not null default false
);
```

---

### 3.2 Tabel riwayat perubahan status: `laporan_masalah_log`

Agar setiap perubahan bisa diaudit, buat tabel log kecil.

```sql
create table public.laporan_masalah_log (
  id bigserial primary key,
  created_at timestamptz not null default now(),
  laporan_id uuid not null references public.laporan_masalah(id) on delete cascade,
  old_status text null,
  new_status text not null,
  changed_by uuid null references auth.users(id),
  catatan text null
);
```

Manfaatnya:

- status tidak berubah diam-diam,
- bisa ditelusuri siapa yang mengubah,
- debugging internal jadi lebih enak.

---

### 3.3 Kenapa `status` lebih baik daripada `is_fixed`

Kalau hanya pakai:

```text
is_fixed = false / true
```

maka sistem hanya tahu:
- belum selesai,
- sudah selesai.

Padahal dalam praktik, sering ada kondisi seperti:

- laporan baru masuk,
- sedang ditinjau,
- sedang diperbaiki,
- sudah diperbaiki,
- ditolak karena bukan bug,
- perlu info tambahan.

Karena itu, `status` lebih tepat.

---

## 4. Status yang Disarankan

Gunakan enumerasi logis berikut:

- `OPEN` → laporan baru masuk
- `IN_PROGRESS` → sedang diperiksa / diperbaiki
- `FIXED` → sudah diperbaiki
- `REJECTED` → tidak valid / bukan bug
- `NEED_INFO` → butuh keterangan tambahan
- `WONT_FIX` → tidak akan diperbaiki untuk sementara

Kalau ingin sederhana, minimal cukup:

- `OPEN`
- `IN_PROGRESS`
- `FIXED`
- `REJECTED`

---

## 5. Data Otomatis yang Dikirim dari Aplikasi Android

Agar debugging lebih cepat, aplikasi sebaiknya mengirim metadata berikut secara otomatis:

- versi aplikasi,
- versi kode / build number,
- versi Android,
- brand perangkat,
- model perangkat,
- manufacturer,
- SDK level,
- bahasa / locale,
- timezone,
- nama akun jika tersedia,
- NIS jika relevan,
- screenshot jika pengguna mengizinkan,
- attachment tambahan jika diperlukan.

Contoh payload:

```json
{
  "judul": "Absensi tidak muncul",
  "deskripsi": "Saat membuka halaman absensi, data tidak tampil.",
  "kategori": "BUG",
  "prioritas": "HIGH",
  "app_version": "1.1.0",
  "android_version": "14",
  "device_brand": "OPPO",
  "device_model": "CPH2579",
  "device_manufacturer": "OPPO",
  "device_sdk": 34,
  "locale": "id-ID",
  "timezone": "Asia/Jakarta",
  "source": "android"
}
```

Dengan data seperti ini, admin tidak perlu menebak-nebak lagi laporan terjadi di device apa.

---

## 6. Alur dari Aplikasi Android Kotlin

### 6.1 UI yang disarankan

Menu yang paling ramah untuk user sebaiknya bukan “Laporkan Masalah” saja, tetapi:

- **Bantuan & Masukan**
  - Laporkan Bug
  - Usulkan Fitur
  - Pertanyaan
  - Masukan

Jika ingin tetap sederhana, cukup gunakan:
- **Laporkan Masalah**

Tetapi di dalamnya buat subkategori.

### 6.2 Field yang diisi user

Di aplikasi Android, user cukup mengisi:

- judul,
- deskripsi,
- kategori,
- prioritas,
- screenshot opsional.

Sisanya diisi otomatis oleh aplikasi.

### 6.3 Hal yang dikirim otomatis

Sebelum request dikirim ke database, aplikasi mengumpulkan:
- app version,
- device info,
- Android version,
- locale,
- timezone.

### 6.4 Flow pengiriman

```text
User buka menu bantuan
↓
User tulis masalah
↓
Aplikasi ambil device info otomatis
↓
Aplikasi kirim ke Supabase
↓
Database simpan laporan
↓
Admin panel menampilkan laporan baru
```

---

## 7. Implementasi Kotlin yang Disarankan

### 7.1 Data class laporan

```kotlin
data class LaporanMasalahRequest(
    val judul: String,
    val deskripsi: String,
    val kategori: String,
    val prioritas: String,
    val appVersion: String?,
    val androidVersion: String?,
    val deviceBrand: String?,
    val deviceModel: String?,
    val deviceManufacturer: String?,
    val deviceSdk: Int?,
    val locale: String?,
    val timezone: String?,
    val screenshotUrl: String? = null,
    val attachmentPaths: List<String> = emptyList()
)
```

### 7.2 Sumber device info

Ambil dari:
- `Build.VERSION.RELEASE`
- `Build.MODEL`
- `Build.BRAND`
- `Build.MANUFACTURER`
- `Build.VERSION.SDK_INT`
- `Locale.getDefault()`
- `TimeZone.getDefault().id`

### 7.3 Prinsip UX di Android

- form dibuat sesingkat mungkin,
- jangan paksa user mengisi data teknis,
- jika bisa, screenshot opsional,
- tampilkan status pengiriman dengan jelas,
- setelah berhasil kirim, tampilkan nomor tiket atau pesan sukses.

### 7.4 UX setelah submit

Tampilkan:
- “Laporan berhasil dikirim”
- nomor tiket
- status awal: `OPEN`
- estimasi tindak lanjut bila ada

---

## 8. Implementasi Database yang Disarankan

### 8.1 Insert laporan

Gunakan insert biasa dengan validasi RLS dan policy yang jelas.

### 8.2 Constraint yang perlu dijaga

- judul tidak boleh kosong,
- deskripsi tidak boleh kosong,
- status hanya boleh dari daftar yang disetujui,
- prioritas hanya boleh dari daftar yang disetujui.

### 8.3 Index yang disarankan

Tambahkan index untuk mempercepat dashboard admin:

```sql
create index if not exists laporan_masalah_status_idx
on public.laporan_masalah(status);

create index if not exists laporan_masalah_created_at_idx
on public.laporan_masalah(created_at desc);

create index if not exists laporan_masalah_user_id_idx
on public.laporan_masalah(user_id);
```

### 8.4 RLS

RLS harus dibuat hati-hati:
- user hanya boleh melihat laporan miliknya,
- admin boleh melihat semua laporan.

---

## 9. Alur di Admin Panel

Admin panel sebaiknya menjadi pusat monitoring.

### 9.1 Tampilan daftar laporan

Di halaman admin, tampilkan:

- jumlah OPEN,
- jumlah IN_PROGRESS,
- jumlah FIXED,
- jumlah REJECTED,
- daftar terbaru di atas,
- filter berdasarkan status,
- filter berdasarkan kategori,
- filter berdasarkan prioritas,
- filter berdasarkan device atau app version.

### 9.2 Detail laporan

Saat satu laporan dibuka, tampilkan:

- judul,
- deskripsi,
- user / santri / wali,
- kategori,
- prioritas,
- status,
- versi aplikasi,
- device info,
- screenshot,
- attachment,
- catatan admin,
- riwayat status.

### 9.3 Aksi admin

Admin dapat:
- ubah status,
- beri catatan,
- tandai fixed,
- tandai butuh info,
- tandai ditolak,
- lihat riwayat perubahan.

### 9.4 Catatan admin yang baik

Catatan admin sebaiknya singkat, jelas, dan berguna.

Contoh:
- “Sudah diperbaiki di versi 1.1.1.”
- “Butuh screenshot tambahan.”
- “Bukan bug, melainkan data belum tersinkron.”
- “Sedang dianalisis di device OPPO seri ini.”

---

## 10. Alur Status yang Disarankan

```text
OPEN
↓
IN_PROGRESS
↓
FIXED
```

Atau jika tidak valid:

```text
OPEN
↓
REJECTED
```

Jika butuh data tambahan:

```text
OPEN
↓
NEED_INFO
↓
IN_PROGRESS
↓
FIXED
```

---

## 11. Fitur yang Sangat Membantu Debugging

### 11.1 Screenshot otomatis
Jika user mengizinkan, screenshot sangat membantu.

### 11.2 Versi aplikasi otomatis
Ini wajib, karena banyak bug bergantung pada versi release.

### 11.3 Informasi device otomatis
Ini sangat penting untuk kasus seperti:
- paket installer gagal,
- bug layout di device tertentu,
- konflik OEM,
- crash pada Android tertentu.

### 11.4 Nomor tiket
Setiap laporan sebaiknya punya ID yang mudah disebut.

Contoh:
- `BUG-20260727-001`

### 11.5 Riwayat perubahan status
Ini mempermudah tracking dan audit.

---

## 12. Contoh Skema Data yang Lebih Matang

Jika ingin lebih profesional, dapat ditambah tabel pendukung:

### `laporan_masalah_attachments`
Untuk file screenshot dan lampiran tambahan.

### `laporan_masalah_comments`
Untuk percakapan antara admin dan pelapor.

### `laporan_masalah_tags`
Untuk tag seperti:
- UI
- Keuangan
- Absensi
- Login
- Notifikasi
- Instalasi

### `laporan_masalah_assignment`
Jika nanti laporan perlu ditugaskan ke admin tertentu.

Namun untuk awal, cukup tabel utama + log status.

---

## 13. Rekomendasi Struktur Minimal yang Siap Dipakai

Kalau ingin langsung sederhana tetapi rapi, saya sarankan:

### Tabel utama
- `laporan_masalah`

### Tabel log
- `laporan_masalah_log`

### Status utama
- `OPEN`
- `IN_PROGRESS`
- `FIXED`
- `REJECTED`

### Data otomatis dari Android
- app version
- Android version
- brand
- model
- manufacturer
- SDK
- locale
- timezone

### Admin panel
- list laporan
- detail laporan
- change status
- notes
- filter

---

## 14. Rekomendasi Implementasi Bertahap

### Tahap 1
Buat form sederhana di Android:
- judul
- deskripsi
- screenshot opsional

### Tahap 2
Simpan laporan ke database dengan metadata device otomatis.

### Tahap 3
Tambahkan admin panel untuk monitoring dan status.

### Tahap 4
Tambahkan log perubahan status dan komentar admin.

### Tahap 5
Tambahkan kategori lebih rinci dan attachment tambahan.

---

## 15. Nilai Besar Fitur Ini

Fitur ini akan membantu:

- mempercepat deteksi bug,
- mempermudah debugging lintas device,
- mengurangi pesan pribadi yang tersebar,
- membuat perbaikan lebih terstruktur,
- memperjelas prioritas masalah,
- memberi user rasa bahwa laporan mereka benar-benar dibaca.

---

## 16. Kesimpulan

Fitur laporan masalah ini sangat layak dibuat, dan bahkan akan terasa sangat bermanfaat kalau diterapkan dengan desain yang disiplin:

- aplikasi Kotlin mengirim laporan + metadata otomatis,
- database menyimpan laporan secara rapi,
- admin panel menjadi tempat monitoring dan tindak lanjut,
- status dilacak dengan jelas,
- riwayat perubahan tersimpan,
- proses debugging menjadi jauh lebih mudah.

Kalau dibangun dengan baik, fitur ini akan menjadi salah satu fondasi operasional paling penting di Al-Hasanah Media.
