-- Perbaikan hasil Supabase advisor untuk modul Falak.
-- Bucket falak-ephemeris bersifat publik, sehingga object URL publik tetap bisa
-- dipakai tanpa policy SELECT yang mengizinkan listing seluruh isi bucket.

drop policy if exists "falak_ephemeris_storage_public_read" on storage.objects;

create index if not exists idx_falak_indeks_data_berkas
  on public.falak_indeks_data (berkas_id);

create index if not exists idx_falak_paket_data_dibuat_oleh
  on public.falak_paket_data (dibuat_oleh);

create index if not exists idx_falak_paket_data_diterbitkan_oleh
  on public.falak_paket_data (diterbitkan_oleh);
