create index if not exists idx_murojaah_dicatat_oleh
  on public.murojaah_tahfidz (dicatat_oleh_id)
  where dicatat_oleh_id is not null;
