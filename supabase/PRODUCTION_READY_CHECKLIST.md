# PRODUCTION READY CHECKLIST - ALHASANAH MEDIA

Dokumen ini adalah panduan akhir untuk memastikan project Supabase Anda siap 100% untuk lingkungan produksi.

## 1. Setup Skema Database (Bootstrap)
File skema baseline telah tersimpan di: `supabase/bootstrap/baseline_production.sql`.
- [ ] **Eksekusi:** Gunakan `psql` dengan connection string dari project produksi baru.
  ```bash
  psql "URL_DATABASE_PRODUKSI_BARU" -f supabase/bootstrap/baseline_production.sql
  ```

## 2. Environment Variables & Secrets
Anda wajib menyalin dan mengonfigurasi Secrets di Dashboard Supabase (Project Settings > Edge Functions) agar Edge Functions berfungsi:

- [ ] `MIDTRANS_SERVER_KEY`
- [ ] `GEMINI_API_KEY`
- [ ] `SUPABASE_SERVICE_ROLE_KEY`
- [ ] `FCM_SERVER_KEY` (atau file service-account.json untuk push notifications)
- [ ] Konfigurasi `Database URL` pada `local.properties` (untuk sisi Android).

## 3. Deployment Edge Functions
Seluruh fungsi ada di `supabase/functions/`. Jalankan perintah ini untuk deploy ke project baru:

```bash
# Link project baru (hanya sekali)
supabase link --project-ref <PROJECT_ID_PRODUKSI>

# Deploy semua fungsi
supabase functions deploy
```
*Catatan: Pastikan `config.toml` sudah disesuaikan (terutama `verify_jwt` untuk fungsi publik vs private).*

## 4. Keamanan & Kesiapan Produksi (Wajib)
- [ ] **RLS Policies:** Verifikasi bahwa setiap tabel di skema `public` sudah mengaktifkan RLS dan memiliki kebijakan yang tepat (`SELECT`, `INSERT`, `UPDATE`, `DELETE`).
- [ ] **Auth Settings:**
    - [ ] Konfigurasi `Site URL` dan `Redirect URLs` di Dashboard (Auth > URL Configuration).
    - [ ] Pastikan SMTP diatur (jangan gunakan email default Supabase untuk produksi).
- [ ] **Data API:** Pastikan semua tabel yang diperlukan aplikasi sudah di-expose di API (jika menggunakan Auto-schema).
- [ ] **Extensions:** Pastikan `pg_cron`, `pg_vector`, dan `postgis` aktif di database produksi.

## 5. Validasi Akhir
- [ ] Lakukan tes login (Wali/Santri/Alumni).
- [ ] Cek transaksi keuangan (Pastikan callback Midtrans tersinkronisasi).
- [ ] Cek notifikasi push (Pastikan FCM terintegrasi).

---
*Catatan: Jangan pernah melakukan commit file yang berisi secret key (seperti .env) ke dalam repositori git.*
