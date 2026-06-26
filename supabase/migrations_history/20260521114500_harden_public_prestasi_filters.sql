alter table public.prestasi_santri
  drop constraint if exists prestasi_santri_kategori_check;

alter table public.prestasi_santri
  add constraint prestasi_santri_kategori_check
  check (
    kategori in (
      'TAHFIDZ',
      'KITAB',
      'KHATAM',
      'AKADEMIK',
      'LOMBA',
      'AKHLAK',
      'OLAHRAGA',
      'SENI',
      'UMUM',
      'LAINNYA'
    )
  );

drop function if exists public.get_public_prestasi_santri();
drop function if exists public.get_public_prestasi_santri(text, text, integer, integer);

create or replace function public.get_public_prestasi_santri(
  p_kategori text default null,
  p_search text default null,
  p_limit integer default 50,
  p_offset integer default 0
)
returns table (
  prestasi_id bigint,
  santri_nama text,
  santri_kelas text,
  santri_jurusan text,
  kategori text,
  judul_prestasi text,
  keterangan text,
  tanggal_prestasi date,
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
    p.poin_prestasi
  from public.prestasi_santri p
  join public.santri s on s.nis = p.santri_nis
  where coalesce(s.status_santri::text, 'AKTIF') in ('AKTIF', 'LULUS', 'ALUMNI')
    and (p_kategori is null or p.kategori = p_kategori)
    and (
      nullif(trim(p_search), '') is null
      or s.nama ilike '%' || trim(p_search) || '%'
      or s.kelas::text ilike '%' || trim(p_search) || '%'
      or s.jurusan::text ilike '%' || trim(p_search) || '%'
      or p.kategori ilike '%' || trim(p_search) || '%'
      or p.judul_prestasi ilike '%' || trim(p_search) || '%'
    )
  order by p.tanggal_prestasi desc nulls last, p.id desc
  limit greatest(1, least(coalesce(p_limit, 50), 100))
  offset greatest(0, coalesce(p_offset, 0));
$$;

revoke all on function public.get_public_prestasi_santri(text, text, integer, integer) from public;
grant execute on function public.get_public_prestasi_santri(text, text, integer, integer) to anon, authenticated;

create index if not exists idx_prestasi_public_filter
  on public.prestasi_santri (kategori, tanggal_prestasi desc, id desc);
