-- Improve wali-facing notification copy for student activity events.
-- This keeps the existing notification_queue pipeline and trigger names intact.

create or replace function public.tr_notify_pelanggaran()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  v_santri record;
  v_jenis text;
  v_body text;
begin
  select s.nama, s.wali_id
    into v_santri
  from public.santri s
  where s.nis = new.santri_nis;

  if v_santri.wali_id is null then
    return new;
  end if;

  v_jenis := nullif(initcap(replace(trim(coalesce(new.jenis_pelanggaran, '')), '_', ' ')), '');

  v_body := concat(
    'Assalamu''alaikum, ada catatan pembinaan untuk ',
    coalesce(v_santri.nama, 'ananda'),
    case when v_jenis is not null then ': ' || v_jenis else '.' end,
    '. Mohon Bapak/Ibu mendampingi ananda dengan tenang. Detail tersedia di menu Kedisiplinan.'
  );

  insert into public.notification_queue (
    user_id,
    title,
    body,
    data,
    source_table,
    event_type,
    priority,
    channel,
    reference_id,
    scheduled_at
  )
  values (
    v_santri.wali_id,
    'Catatan Pembinaan Ananda',
    left(v_body, 500),
    jsonb_build_object(
      'type', 'pelanggaran',
      'santri_nis', new.santri_nis,
      'santri_nama', v_santri.nama,
      'pelanggaran_id', new.id,
      'jenis_pelanggaran', new.jenis_pelanggaran,
      'poin', coalesce(new.poin, 0),
      'tanggal', new.tanggal,
      'deeplink', 'alhasanah://pelanggaran/' || new.id::text,
      'automatic', true
    ),
    'pelanggaran_santri',
    'pelanggaran.created',
    'normal',
    'push',
    new.id::text,
    now()
  );

  return new;
end;
$function$;

create or replace function public.tr_notify_perizinan()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  v_santri record;
  v_status_raw text;
  v_status_label text;
  v_kind text;
  v_body text;
begin
  if not ((tg_op = 'INSERT') or (old.status is distinct from new.status)) then
    return new;
  end if;

  select s.nama, s.wali_id
    into v_santri
  from public.santri s
  where s.nis = new.santri_nis;

  if v_santri.wali_id is null then
    return new;
  end if;

  v_status_raw := lower(trim(coalesce(new.status, '')));
  v_status_label := case
    when v_status_raw in ('approved', 'approve', 'disetujui', 'diizinkan', 'diterima') then 'sudah disetujui'
    when v_status_raw in ('rejected', 'reject', 'ditolak', 'tidak disetujui') then 'belum dapat disetujui'
    when v_status_raw in ('pending', 'diproses', 'menunggu', 'proses', 'review') then 'sedang ditinjau oleh pengurus'
    when v_status_raw in ('kembali', 'sudah kembali', 'selesai') then 'sudah kembali ke pesantren'
    when v_status_raw in ('keluar', 'berangkat', 'izin keluar') then 'sedang dalam izin keluar'
    when v_status_raw = '' then 'sudah diperbarui oleh pengurus'
    else 'sudah diperbarui oleh pengurus'
  end;

  v_kind := nullif(initcap(replace(trim(coalesce(new.jenis_izin, '')), '_', ' ')), '');

  v_body := concat(
    'Assalamu''alaikum, izin ',
    coalesce(v_kind, 'santri'),
    ' untuk ',
    coalesce(v_santri.nama, 'ananda'),
    ' ',
    v_status_label,
    '. Silakan lihat detailnya di menu Perizinan.'
  );

  insert into public.notification_queue (
    user_id,
    title,
    body,
    data,
    source_table,
    event_type,
    priority,
    channel,
    reference_id,
    scheduled_at
  )
  values (
    v_santri.wali_id,
    'Kabar Perizinan Ananda',
    left(v_body, 500),
    jsonb_build_object(
      'type', 'perizinan',
      'santri_nis', new.santri_nis,
      'santri_nama', v_santri.nama,
      'perizinan_id', new.id,
      'jenis_izin', new.jenis_izin,
      'status', new.status,
      'status_label', v_status_label,
      'tanggal', new.tanggal,
      'tanggal_kembali', new.tanggal_kembali,
      'deeplink', 'alhasanah://perizinan/' || new.id::text,
      'automatic', true
    ),
    'perizinan_santri',
    'perizinan.status_changed',
    'normal',
    'push',
    new.id::text,
    now()
  );

  return new;
end;
$function$;

create or replace function public.tr_notify_kesehatan()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  v_santri record;
  v_keluhan text;
  v_tindakan text;
  v_body text;
begin
  select s.nama, s.wali_id
    into v_santri
  from public.santri s
  where s.nis = new.santri_nis;

  if v_santri.wali_id is null then
    return new;
  end if;

  v_keluhan := nullif(trim(coalesce(new.keluhan, '')), '');
  v_tindakan := nullif(trim(coalesce(new.tindakan, '')), '');

  v_body := concat(
    'Assalamu''alaikum, ',
    coalesce(v_santri.nama, 'ananda'),
    ' sedang mendapat perhatian dari petugas kesehatan.',
    case when v_keluhan is not null then ' Keluhan: ' || left(v_keluhan, 120) || '.' else '' end,
    case when v_tindakan is not null then ' Penanganan: ' || left(v_tindakan, 120) || '.' else '' end,
    ' Mohon doa agar ananda tetap sehat.'
  );

  insert into public.notification_queue (
    user_id,
    title,
    body,
    data,
    source_table,
    event_type,
    priority,
    channel,
    reference_id,
    scheduled_at
  )
  values (
    v_santri.wali_id,
    'Kabar Kesehatan Ananda',
    left(v_body, 500),
    jsonb_build_object(
      'type', 'kesehatan',
      'santri_nis', new.santri_nis,
      'santri_nama', v_santri.nama,
      'kesehatan_id', new.id,
      'tanggal', new.tanggal,
      'has_keluhan', v_keluhan is not null,
      'has_tindakan', v_tindakan is not null,
      'deeplink', 'alhasanah://kesehatan/' || new.id::text,
      'automatic', true
    ),
    'kesehatan_santri',
    'kesehatan.created',
    'normal',
    'push',
    new.id::text,
    now()
  );

  return new;
end;
$function$;

create or replace function public.notify_wali_on_prestasi_created()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  v_santri record;
  v_title text;
  v_prestasi text;
  v_body text;
begin
  select
    s.nama,
    s.kelas::text as kelas,
    s.jurusan::text as jurusan,
    s.wali_id
  into v_santri
  from public.santri s
  where s.nis = new.santri_nis;

  if v_santri.wali_id is null then
    return new;
  end if;

  if exists (
    select 1
    from public.notification_queue q
    where q.user_id = v_santri.wali_id
      and q.source_table = 'prestasi_santri'
      and q.event_type = 'prestasi.created'
      and q.reference_id = new.id::text
  ) then
    return new;
  end if;

  v_title := 'Prestasi Ananda';
  v_prestasi := nullif(trim(coalesce(new.judul_prestasi, '')), '');
  v_body := concat(
    'Alhamdulillah, ',
    coalesce(v_santri.nama, 'ananda'),
    ' meraih prestasi',
    case when v_prestasi is not null then ': ' || left(v_prestasi, 140) else '' end,
    '. Terima kasih atas doa dan dukungan Bapak/Ibu.'
  );

  insert into public.notification_queue (
    user_id,
    title,
    body,
    data,
    source_table,
    event_type,
    priority,
    channel,
    reference_id,
    scheduled_at
  )
  values (
    v_santri.wali_id,
    v_title,
    left(v_body, 500),
    jsonb_build_object(
      'type', 'prestasi_created',
      'prestasi_id', new.id,
      'santri_nis', new.santri_nis,
      'santri_nama', v_santri.nama,
      'santri_kelas', v_santri.kelas,
      'santri_jurusan', v_santri.jurusan,
      'kategori', new.kategori,
      'judul_prestasi', new.judul_prestasi,
      'tanggal_prestasi', new.tanggal_prestasi,
      'poin_prestasi', coalesce(new.poin_prestasi, 0),
      'deeplink', 'alhasanah://prestasi/' || new.id::text,
      'automatic', true
    ),
    'prestasi_santri',
    'prestasi.created',
    'normal',
    'push',
    new.id::text,
    now()
  );

  return new;
end;
$function$;

create index if not exists idx_notification_queue_prestasi_created_dedupe
  on public.notification_queue (event_type, reference_id, user_id)
  where source_table = 'prestasi_santri'
    and event_type = 'prestasi.created'
    and reference_id is not null;

revoke execute on function public.tr_notify_pelanggaran() from public, anon, authenticated;
revoke execute on function public.tr_notify_perizinan() from public, anon, authenticated;
revoke execute on function public.tr_notify_kesehatan() from public, anon, authenticated;
revoke execute on function public.notify_wali_on_prestasi_created() from public, anon, authenticated;

grant execute on function public.tr_notify_pelanggaran() to service_role;
grant execute on function public.tr_notify_perizinan() to service_role;
grant execute on function public.tr_notify_kesehatan() to service_role;
grant execute on function public.notify_wali_on_prestasi_created() to service_role;
