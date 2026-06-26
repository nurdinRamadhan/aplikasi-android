-- Production-readiness cleanup that avoids changing Android app contracts.
-- PostGIS estimated extent helpers are not used by the Android client and should
-- not be executable through the public Data API roles.
revoke execute on function public.st_estimatedextent(text, text) from public, anon, authenticated;
revoke execute on function public.st_estimatedextent(text, text, text) from public, anon, authenticated;
revoke execute on function public.st_estimatedextent(text, text, text, boolean) from public, anon, authenticated;
grant execute on function public.st_estimatedextent(text, text) to service_role;
grant execute on function public.st_estimatedextent(text, text, text) to service_role;
grant execute on function public.st_estimatedextent(text, text, text, boolean) to service_role;

-- Cover foreign keys that appear in Android/admin read paths or high-churn tables.
create index if not exists idx_berita_penulis_id
on public.berita (penulis_id)
where penulis_id is not null;

create index if not exists idx_hafalan_kitab_dicatat_oleh_id
on public.hafalan_kitab (dicatat_oleh_id)
where dicatat_oleh_id is not null;

create index if not exists idx_hafalan_tahfidz_dicatat_oleh_id
on public.hafalan_tahfidz (dicatat_oleh_id)
where dicatat_oleh_id is not null;

create index if not exists idx_kesehatan_santri_nis
on public.kesehatan_santri (santri_nis);

create index if not exists idx_pelanggaran_santri_nis
on public.pelanggaran_santri (santri_nis);

create index if not exists idx_perizinan_santri_nis
on public.perizinan_santri (santri_nis);

create index if not exists idx_tagihan_santri_jenis_pembayaran_id
on public.tagihan_santri (jenis_pembayaran_id)
where jenis_pembayaran_id is not null;

create index if not exists idx_transaksi_keuangan_admin_pencatat_id
on public.transaksi_keuangan (admin_pencatat_id)
where admin_pencatat_id is not null;

create index if not exists idx_transaksi_keuangan_santri_nis
on public.transaksi_keuangan (santri_nis)
where santri_nis is not null;

create index if not exists idx_transaksi_keuangan_wali_id
on public.transaksi_keuangan (wali_id)
where wali_id is not null;

create index if not exists idx_detail_transaksi_tagihan_id
on public.detail_transaksi (tagihan_id)
where tagihan_id is not null;

-- Drop indexes that duplicate an existing equivalent index.
drop index if exists public.idx_prestasi_public_filter;
alter table if exists public.transaksi_keuangan
    drop constraint if exists unique_midtrans_order_id;
drop index if exists public.idx_transaksi_order_id;
