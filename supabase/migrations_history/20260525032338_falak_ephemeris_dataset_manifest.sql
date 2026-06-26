-- Falak ephemeris dataset registry.
-- Large JSON/PDF files live in Supabase Storage. Postgres stores the
-- searchable manifest, versioning metadata, glossary, and lightweight index.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'falak-ephemeris',
  'falak-ephemeris',
  true,
  104857600,
  array[
    'application/json',
    'application/gzip',
    'application/pdf',
    'text/plain'
  ]
)
on conflict (id) do update
set public = true,
    file_size_limit = 104857600,
    allowed_mime_types = array[
      'application/json',
      'application/gzip',
      'application/pdf',
      'text/plain'
    ];

create table if not exists public.falak_paket_data (
  id uuid primary key default gen_random_uuid(),
  kode text not null unique,
  judul text not null,
  deskripsi text,
  tahun integer not null check (tahun between 1900 and 2200),
  versi text not null,
  jenis_sumber text not null default 'kemenag'
    check (jenis_sumber in ('kemenag', 'jpl', 'swiss', 'winhisab', 'pesantren', 'manual', 'lainnya')),
  sumber_resmi text not null,
  tautan_sumber text,
  bahasa text not null default 'id',
  zona_waktu_data text not null default 'UT',
  status text not null default 'draf'
    check (status in ('draf', 'aktif', 'arsip', 'ditarik')),
  path_manifest_storage text,
  sha256_pdf text,
  sha256_manifest text,
  ukuran_total_bytes bigint not null default 0 check (ukuran_total_bytes >= 0),
  jumlah_halaman integer not null default 0 check (jumlah_halaman >= 0),
  jumlah_hari_ephemeris integer not null default 0 check (jumlah_hari_ephemeris >= 0),
  jumlah_tabel_hilal integer not null default 0 check (jumlah_tabel_hilal >= 0),
  jumlah_baris_indeks integer not null default 0 check (jumlah_baris_indeks >= 0),
  tanggal_mulai date,
  tanggal_selesai date,
  catatan_pembaruan text,
  metadata jsonb not null default '{}'::jsonb,
  dibuat_oleh uuid references public.profiles(id) on delete set null,
  diterbitkan_oleh uuid references public.profiles(id) on delete set null,
  diterbitkan_pada timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint falak_paket_data_rentang_tanggal_valid
    check (tanggal_mulai is null or tanggal_selesai is null or tanggal_mulai <= tanggal_selesai)
);

create unique index if not exists uq_falak_paket_data_sumber_tahun_aktif
on public.falak_paket_data (jenis_sumber, tahun)
where status = 'aktif';

create index if not exists idx_falak_paket_data_status_tahun
on public.falak_paket_data (status, tahun desc, jenis_sumber);

create index if not exists idx_falak_paket_data_metadata_gin
on public.falak_paket_data using gin (metadata);

create table if not exists public.falak_berkas_data (
  id uuid primary key default gen_random_uuid(),
  paket_id uuid not null references public.falak_paket_data(id) on delete cascade,
  jenis_berkas text not null
    check (jenis_berkas in (
      'manifest',
      'ephemeris_harian',
      'hilal_lokasi',
      'halaman_pdf',
      'pdf_sumber',
      'indeks_pencarian',
      'istilah',
      'lainnya'
    )),
  nama_berkas text not null,
  nama_tampil text not null,
  deskripsi text,
  path_storage text not null unique,
  mime_type text not null,
  ukuran_bytes bigint not null check (ukuran_bytes >= 0),
  sha256 text not null,
  jumlah_record integer not null default 0 check (jumlah_record >= 0),
  wajib_diunduh boolean not null default false,
  urutan integer not null default 0,
  status text not null default 'aktif'
    check (status in ('aktif', 'arsip', 'ditarik')),
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_falak_berkas_data_paket_jenis
on public.falak_berkas_data (paket_id, jenis_berkas, urutan);

create index if not exists idx_falak_berkas_data_status
on public.falak_berkas_data (status, jenis_berkas);

create table if not exists public.falak_indeks_data (
  id uuid primary key default gen_random_uuid(),
  paket_id uuid not null references public.falak_paket_data(id) on delete cascade,
  berkas_id uuid references public.falak_berkas_data(id) on delete cascade,
  tipe_indeks text not null
    check (tipe_indeks in (
      'tanggal',
      'jam_ut',
      'matahari',
      'bulan',
      'hilal',
      'lokasi',
      'halaman_pdf',
      'bulan_hijriah',
      'istilah',
      'contoh_perhitungan',
      'lainnya'
    )),
  judul text not null,
  ringkasan text,
  kata_kunci text[] not null default '{}'::text[],
  tanggal_data date,
  jam_ut integer check (jam_ut between 0 and 24),
  nama_lokasi text,
  nomor_halaman_pdf integer check (nomor_halaman_pdf is null or nomor_halaman_pdf > 0),
  path_json_pointer text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_falak_indeks_data_paket_tipe
on public.falak_indeks_data (paket_id, tipe_indeks, tanggal_data, jam_ut);

create index if not exists idx_falak_indeks_data_lokasi
on public.falak_indeks_data (paket_id, lower(nama_lokasi))
where nama_lokasi is not null;

create index if not exists idx_falak_indeks_data_kata_kunci_gin
on public.falak_indeks_data using gin (kata_kunci);

create index if not exists idx_falak_indeks_data_metadata_gin
on public.falak_indeks_data using gin (metadata);

create table if not exists public.falak_istilah_ephemeris (
  id uuid primary key default gen_random_uuid(),
  kode_istilah text not null unique,
  nama_tampil text not null,
  nama_pdf text,
  kategori text not null default 'umum'
    check (kategori in ('matahari', 'bulan', 'hilal', 'waktu', 'lokasi', 'perhitungan', 'umum')),
  satuan_umum text,
  penjelasan_awam text not null,
  penjelasan_santri text not null,
  penjelasan_praktisi text not null,
  contoh_nilai text,
  urutan integer not null default 0,
  aktif boolean not null default true,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_falak_istilah_ephemeris_kategori
on public.falak_istilah_ephemeris (kategori, urutan, nama_tampil);

alter table public.falak_paket_data enable row level security;
alter table public.falak_berkas_data enable row level security;
alter table public.falak_indeks_data enable row level security;
alter table public.falak_istilah_ephemeris enable row level security;

drop policy if exists "Falak paket aktif bisa dibaca publik" on public.falak_paket_data;
create policy "Falak paket aktif bisa dibaca publik"
on public.falak_paket_data for select
using (status = 'aktif');

drop policy if exists "Falak paket admin baca semua" on public.falak_paket_data;
create policy "Falak paket admin baca semua"
on public.falak_paket_data for select to authenticated
using (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan', 'kesantrian']));

drop policy if exists "Falak paket admin tulis" on public.falak_paket_data;
create policy "Falak paket admin tulis"
on public.falak_paket_data for all to authenticated
using (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan']))
with check (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan']));

drop policy if exists "Falak berkas aktif bisa dibaca publik" on public.falak_berkas_data;
create policy "Falak berkas aktif bisa dibaca publik"
on public.falak_berkas_data for select
using (
  status = 'aktif'
  and exists (
    select 1
    from public.falak_paket_data p
    where p.id = falak_berkas_data.paket_id
      and p.status = 'aktif'
  )
);

drop policy if exists "Falak berkas admin baca semua" on public.falak_berkas_data;
create policy "Falak berkas admin baca semua"
on public.falak_berkas_data for select to authenticated
using (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan', 'kesantrian']));

drop policy if exists "Falak berkas admin tulis" on public.falak_berkas_data;
create policy "Falak berkas admin tulis"
on public.falak_berkas_data for all to authenticated
using (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan']))
with check (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan']));

drop policy if exists "Falak indeks aktif bisa dibaca publik" on public.falak_indeks_data;
create policy "Falak indeks aktif bisa dibaca publik"
on public.falak_indeks_data for select
using (
  exists (
    select 1
    from public.falak_paket_data p
    where p.id = falak_indeks_data.paket_id
      and p.status = 'aktif'
  )
);

drop policy if exists "Falak indeks admin baca semua" on public.falak_indeks_data;
create policy "Falak indeks admin baca semua"
on public.falak_indeks_data for select to authenticated
using (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan', 'kesantrian']));

drop policy if exists "Falak indeks admin tulis" on public.falak_indeks_data;
create policy "Falak indeks admin tulis"
on public.falak_indeks_data for all to authenticated
using (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan']))
with check (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan']));

drop policy if exists "Falak istilah aktif bisa dibaca publik" on public.falak_istilah_ephemeris;
create policy "Falak istilah aktif bisa dibaca publik"
on public.falak_istilah_ephemeris for select
using (aktif = true);

drop policy if exists "Falak istilah admin tulis" on public.falak_istilah_ephemeris;
create policy "Falak istilah admin tulis"
on public.falak_istilah_ephemeris for all to authenticated
using (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan']))
with check (public.is_admin_in_roles(array['super_admin', 'rois', 'dewan']));

drop policy if exists falak_ephemeris_storage_public_read on storage.objects;
create policy falak_ephemeris_storage_public_read
on storage.objects for select
using (bucket_id = 'falak-ephemeris');

drop policy if exists falak_ephemeris_storage_admin_insert on storage.objects;
create policy falak_ephemeris_storage_admin_insert
on storage.objects for insert to authenticated
with check (
  bucket_id = 'falak-ephemeris'
  and public.is_admin_in_roles(array['super_admin', 'rois', 'dewan'])
);

drop policy if exists falak_ephemeris_storage_admin_update on storage.objects;
create policy falak_ephemeris_storage_admin_update
on storage.objects for update to authenticated
using (
  bucket_id = 'falak-ephemeris'
  and public.is_admin_in_roles(array['super_admin', 'rois', 'dewan'])
)
with check (
  bucket_id = 'falak-ephemeris'
  and public.is_admin_in_roles(array['super_admin', 'rois', 'dewan'])
);

drop policy if exists falak_ephemeris_storage_admin_delete on storage.objects;
create policy falak_ephemeris_storage_admin_delete
on storage.objects for delete to authenticated
using (
  bucket_id = 'falak-ephemeris'
  and public.is_admin_in_roles(array['super_admin', 'rois', 'dewan'])
);

insert into public.falak_istilah_ephemeris (
  kode_istilah,
  nama_tampil,
  nama_pdf,
  kategori,
  satuan_umum,
  penjelasan_awam,
  penjelasan_santri,
  penjelasan_praktisi,
  contoh_nilai,
  urutan
) values
  (
    'bujur_ekliptika_matahari',
    'Bujur Ekliptika Matahari',
    'Apparent Ecliptic Longitude',
    'matahari',
    'derajat',
    'Posisi Matahari pada jalur ekliptika yang tampak dari Bumi.',
    'Dalam falak sering dipakai sebagai data dasar Matahari untuk ijtimak, gerhana, dan awal bulan.',
    'Longitude geosentris apparent Matahari pada sistem ekliptika, disajikan per jam UT.',
    '280° 34'' 07"',
    10
  ),
  (
    'deklinasi_matahari',
    'Deklinasi Matahari',
    'Apparent Declination',
    'matahari',
    'derajat',
    'Jarak Matahari dari ekuator langit ke arah utara atau selatan.',
    'Nilai ini dipakai dalam perhitungan waktu shalat, tinggi Matahari, dan azimut.',
    'Declination apparent geosentris Matahari; nilai negatif berarti selatan ekuator langit.',
    '-23° 01'' 02"',
    20
  ),
  (
    'perata_waktu',
    'Perata Waktu',
    'Equation of Time',
    'waktu',
    'menit dan detik',
    'Selisih antara waktu Matahari rata-rata dan Matahari hakiki.',
    'Dipakai untuk mencari meridian pass atau waktu zawal dalam hisab.',
    'Equation of Time per jam UT, disimpan juga dalam total detik dan jam desimal.',
    '-03m 20s',
    30
  ),
  (
    'bujur_bulan',
    'Bujur Bulan',
    'Apparent Longitude',
    'bulan',
    'derajat',
    'Posisi Bulan pada jalur ekliptika yang tampak dari Bumi.',
    'Dipakai untuk mencari ijtimak dan selisih posisi Bulan-Matahari.',
    'Apparent geocentric ecliptic longitude of the Moon per jam UT.',
    '66° 43'' 01"',
    40
  ),
  (
    'lintang_bulan',
    'Lintang Bulan',
    'Apparent Latitude',
    'bulan',
    'derajat',
    'Jarak Bulan dari ekliptika ke arah utara atau selatan.',
    'Penting untuk menilai kemungkinan gerhana dan posisi hilal.',
    'Apparent geocentric ecliptic latitude of the Moon; tanda menentukan sisi utara/selatan ekliptika.',
    '5° 02'' 57"',
    50
  ),
  (
    'horizontal_paralaks_bulan',
    'Horizontal Paralaks Bulan',
    'Horizontal Parallax',
    'bulan',
    'menit busur',
    'Besaran koreksi posisi Bulan karena pengamat berada di permukaan Bumi.',
    'Dipakai dalam perhitungan tinggi hilal mar’i dan gerhana.',
    'Equatorial horizontal parallax of the Moon, digunakan untuk koreksi topocentric.',
    '00'' 44"',
    60
  ),
  (
    'semi_diameter_bulan',
    'Semi Diameter Bulan',
    'Semi Diameter',
    'bulan',
    'menit busur',
    'Jari-jari tampak piringan Bulan.',
    'Dipakai dalam koreksi tinggi hilal dan batas kontak gerhana.',
    'Apparent semidiameter of the Moon.',
    '16'' 32,93"',
    70
  ),
  (
    'fraksi_illumination_bulan',
    'Fraksi Iluminasi Bulan',
    'Fraction Illumination',
    'bulan',
    'persen',
    'Persentase bagian piringan Bulan yang terkena cahaya Matahari dan tampak dari Bumi.',
    'Data ini membantu membaca keadaan cahaya hilal.',
    'Illuminated fraction of lunar disk as seen from Earth.',
    '91,40%',
    80
  )
on conflict (kode_istilah) do update
set
  nama_tampil = excluded.nama_tampil,
  nama_pdf = excluded.nama_pdf,
  kategori = excluded.kategori,
  satuan_umum = excluded.satuan_umum,
  penjelasan_awam = excluded.penjelasan_awam,
  penjelasan_santri = excluded.penjelasan_santri,
  penjelasan_praktisi = excluded.penjelasan_praktisi,
  contoh_nilai = excluded.contoh_nilai,
  urutan = excluded.urutan,
  aktif = true,
  updated_at = now();

grant select on public.falak_paket_data to anon, authenticated;
grant select on public.falak_berkas_data to anon, authenticated;
grant select on public.falak_indeks_data to anon, authenticated;
grant select on public.falak_istilah_ephemeris to anon, authenticated;

grant insert, update, delete on public.falak_paket_data to authenticated;
grant insert, update, delete on public.falak_berkas_data to authenticated;
grant insert, update, delete on public.falak_indeks_data to authenticated;
grant insert, update, delete on public.falak_istilah_ephemeris to authenticated;
