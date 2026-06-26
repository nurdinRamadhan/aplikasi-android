drop function if exists public.get_public_prestasi_category_counts(text);

create or replace function public.get_public_prestasi_category_counts(
  p_search text default null
)
returns table (
  kategori text,
  total bigint
)
language sql
stable
security definer
set search_path = public
as $$
  select
    p.kategori,
    count(*) as total
  from public.prestasi_santri p
  join public.santri s on s.nis = p.santri_nis
  where coalesce(s.status_santri::text, 'AKTIF') in ('AKTIF', 'LULUS', 'ALUMNI')
    and (
      nullif(trim(p_search), '') is null
      or s.nama ilike '%' || trim(p_search) || '%'
      or s.kelas::text ilike '%' || trim(p_search) || '%'
      or s.jurusan::text ilike '%' || trim(p_search) || '%'
      or p.kategori ilike '%' || trim(p_search) || '%'
      or p.judul_prestasi ilike '%' || trim(p_search) || '%'
    )
  group by p.kategori
  order by p.kategori;
$$;

revoke all on function public.get_public_prestasi_category_counts(text) from public;
grant execute on function public.get_public_prestasi_category_counts(text) to anon, authenticated;
