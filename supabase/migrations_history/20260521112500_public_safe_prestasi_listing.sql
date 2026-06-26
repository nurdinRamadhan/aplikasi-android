drop policy if exists "Public read prestasi santri" on public.prestasi_santri;

revoke select on public.prestasi_santri from anon;

create or replace function public.get_public_prestasi_santri()
returns table (
  prestasi_id bigint,
  santri_nama text,
  santri_kelas text,
  santri_jurusan text,
  kategori text,
  judul_prestasi text,
  keterangan text,
  tanggal_prestasi date,
  sertifikat_url text,
  poin_prestasi integer
)
language sql
stable
security definer
set search_path = public
as $$
  select
    p.id as prestasi_id,
    coalesce(nullif(s.nama, ''), 'Santri Al-Hasanah') as santri_nama,
    s.kelas::text as santri_kelas,
    s.jurusan::text as santri_jurusan,
    p.kategori,
    p.judul_prestasi,
    p.keterangan,
    p.tanggal_prestasi,
    p.sertifikat_url,
    p.poin_prestasi
  from public.prestasi_santri p
  join public.santri s on s.nis = p.santri_nis
  where coalesce(s.status_santri::text, 'AKTIF') in ('AKTIF', 'LULUS', 'ALUMNI')
  order by p.tanggal_prestasi desc nulls last, p.id desc;
$$;

revoke all on function public.get_public_prestasi_santri() from public;
grant execute on function public.get_public_prestasi_santri() to anon, authenticated;

create index if not exists idx_prestasi_santri_tanggal_id
  on public.prestasi_santri (santri_nis, tanggal_prestasi desc, id desc);
