drop policy if exists "Public read prestasi santri" on public.prestasi_santri;

create policy "Public read prestasi santri"
on public.prestasi_santri
for select
to anon, authenticated
using (true);

grant select on public.prestasi_santri to anon, authenticated;

create index if not exists idx_prestasi_tanggal_id
  on public.prestasi_santri (tanggal_prestasi desc, id desc);

create index if not exists idx_prestasi_kategori_tanggal
  on public.prestasi_santri (kategori, tanggal_prestasi desc, id desc);
