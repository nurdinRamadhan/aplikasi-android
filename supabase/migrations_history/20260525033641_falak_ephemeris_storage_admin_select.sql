-- Admin perlu SELECT pada storage.objects agar upload dengan upsert dapat
-- mengganti berkas paket Falak yang sudah ada.

drop policy if exists falak_ephemeris_storage_admin_select on storage.objects;
create policy falak_ephemeris_storage_admin_select
on storage.objects for select to authenticated
using (
  bucket_id = 'falak-ephemeris'
  and public.is_admin_in_roles(array['super_admin', 'rois', 'dewan'])
);
