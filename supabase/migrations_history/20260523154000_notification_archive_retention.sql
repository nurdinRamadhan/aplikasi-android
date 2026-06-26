-- Notification retention for active mobile inbox.
-- Active rows stay in public.notification_queue; expired rows are archived in
-- ops.notification_archive first, then pruned from the public queue.

create table if not exists ops.notification_archive (
  id uuid primary key,
  user_id uuid,
  title text not null,
  body text not null,
  data jsonb not null default '{}'::jsonb,
  status text,
  error_message text,
  source_table text,
  created_at timestamptz,
  sent_at timestamptz,
  event_type text,
  priority text,
  channel text,
  reference_id text,
  read_at timestamptz,
  scheduled_at timestamptz,
  wallet_review_status text,
  wallet_reviewed_by uuid,
  wallet_reviewed_at timestamptz,
  wallet_review_note text,
  archived_at timestamptz not null default now(),
  retention_reason text not null,
  archive_metadata jsonb not null default '{}'::jsonb
);

alter table ops.notification_archive enable row level security;

create index if not exists idx_notification_archive_archived_at
  on ops.notification_archive (archived_at desc);

create index if not exists idx_notification_archive_user_created
  on ops.notification_archive (user_id, created_at desc);

create index if not exists idx_notification_archive_event_type
  on ops.notification_archive (event_type, created_at desc);

create or replace function ops.archive_and_prune_notifications(
  p_dry_run boolean default false,
  p_limit integer default 5000,
  p_sent_read_days integer default 30,
  p_failed_days integer default 14,
  p_critical_days integer default 90
)
returns jsonb
language plpgsql
security definer
set search_path = ops, public, pg_temp
as $$
declare
  v_candidate_count integer := 0;
  v_archived_count integer := 0;
  v_deleted_count integer := 0;
  v_effective_limit integer := least(greatest(coalesce(p_limit, 5000), 1), 50000);
begin
  if p_sent_read_days < 1 or p_failed_days < 1 or p_critical_days < 1 then
    raise exception 'Retention days must be positive';
  end if;

  create temporary table tmp_notification_retention_candidates (
    id uuid primary key,
    retention_reason text not null
  ) on commit drop;

  insert into tmp_notification_retention_candidates (id, retention_reason)
  select q.id,
    case
      when coalesce(q.priority, 'normal') = 'critical'
        then 'critical_reviewed_expired'
      when coalesce(q.status, '') = 'failed'
        then 'failed_expired'
      else 'delivered_or_read_expired'
    end as retention_reason
  from public.notification_queue q
  where (
      coalesce(q.status, '') in ('sent', 'read')
      and coalesce(q.priority, 'normal') <> 'critical'
      and q.created_at < now() - make_interval(days => p_sent_read_days)
    )
    or (
      coalesce(q.status, '') = 'failed'
      and coalesce(q.priority, 'normal') <> 'critical'
      and q.created_at < now() - make_interval(days => p_failed_days)
    )
    or (
      coalesce(q.priority, 'normal') = 'critical'
      and coalesce(q.status, '') in ('sent', 'read', 'failed')
      and coalesce(q.wallet_review_status, '') in ('reviewed', 'resolved', 'ignored_dummy')
      and q.created_at < now() - make_interval(days => p_critical_days)
    )
  order by q.created_at
  limit v_effective_limit;

  get diagnostics v_candidate_count = row_count;

  if not p_dry_run then
    with archived as (
      insert into ops.notification_archive (
        id,
        user_id,
        title,
        body,
        data,
        status,
        error_message,
        source_table,
        created_at,
        sent_at,
        event_type,
        priority,
        channel,
        reference_id,
        read_at,
        scheduled_at,
        wallet_review_status,
        wallet_reviewed_by,
        wallet_reviewed_at,
        wallet_review_note,
        archived_at,
        retention_reason,
        archive_metadata
      )
      select
        q.id,
        q.user_id,
        q.title,
        q.body,
        coalesce(q.data, '{}'::jsonb),
        q.status,
        q.error_message,
        q.source_table,
        q.created_at,
        q.sent_at,
        q.event_type,
        q.priority,
        q.channel,
        q.reference_id,
        q.read_at,
        q.scheduled_at,
        q.wallet_review_status,
        q.wallet_reviewed_by,
        q.wallet_reviewed_at,
        q.wallet_review_note,
        now(),
        c.retention_reason,
        jsonb_build_object(
          'retention_job', 'ops.archive_and_prune_notifications',
          'sent_read_days', p_sent_read_days,
          'failed_days', p_failed_days,
          'critical_days', p_critical_days
        )
      from public.notification_queue q
      join tmp_notification_retention_candidates c on c.id = q.id
      on conflict (id) do nothing
      returning id
    )
    select count(*) into v_archived_count from archived;

    with deleted as (
      delete from public.notification_queue q
      using tmp_notification_retention_candidates c
      where q.id = c.id
        and exists (
          select 1
          from ops.notification_archive a
          where a.id = q.id
        )
      returning 1
    )
    select count(*) into v_deleted_count from deleted;
  end if;

  return jsonb_build_object(
    'dry_run', p_dry_run,
    'candidate_count', v_candidate_count,
    'archived_count', v_archived_count,
    'deleted_count', v_deleted_count,
    'limit', v_effective_limit,
    'retention', jsonb_build_object(
      'sent_read_days', p_sent_read_days,
      'failed_days', p_failed_days,
      'critical_days', p_critical_days
    ),
    'ran_at', now()
  );
end;
$$;

revoke all on function ops.archive_and_prune_notifications(boolean, integer, integer, integer, integer) from public, anon, authenticated;
grant execute on function ops.archive_and_prune_notifications(boolean, integer, integer, integer, integer) to service_role;

do $$
begin
  if exists (select 1 from cron.job where jobname = 'backend-notification-retention-daily') then
    perform cron.unschedule('backend-notification-retention-daily');
  end if;
end $$;

-- 19:00 UTC = 02:00 WIB.
select cron.schedule(
  'backend-notification-retention-daily',
  '0 19 * * *',
  $$ select ops.archive_and_prune_notifications(false, 5000, 30, 14, 90); $$
);
