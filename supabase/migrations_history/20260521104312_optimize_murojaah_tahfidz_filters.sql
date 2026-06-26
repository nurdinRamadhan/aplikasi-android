create index if not exists idx_murojaah_santri_jenis_tanggal
  on public.murojaah_tahfidz (santri_nis, jenis_murojaah, tanggal desc, id desc)
  where jenis_murojaah is not null;

create index if not exists idx_murojaah_santri_predikat_tanggal
  on public.murojaah_tahfidz (santri_nis, predikat, tanggal desc, id desc)
  where predikat is not null;

create index if not exists idx_murojaah_santri_status_tanggal
  on public.murojaah_tahfidz (santri_nis, status, tanggal desc, id desc)
  where status is not null;
