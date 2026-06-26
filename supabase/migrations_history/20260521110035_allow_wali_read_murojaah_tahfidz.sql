drop policy if exists "Wali view own murojaah tahfidz" on public.murojaah_tahfidz;

create policy "Wali view own murojaah tahfidz"
on public.murojaah_tahfidz
for select
to authenticated
using (
  exists (
    select 1
    from public.santri s
    where s.nis = murojaah_tahfidz.santri_nis
      and s.wali_id = auth.uid()
  )
);
