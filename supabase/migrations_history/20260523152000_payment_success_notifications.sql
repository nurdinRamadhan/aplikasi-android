-- Additive payment success notifications.
-- Keeps existing payment flows intact and only enqueues notifications after
-- backend-confirmed success states.

create or replace function public.tr_notify_donasi_success()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_amount numeric := coalesce(new.jumlah, 0);
  v_reference_id text := coalesce(new.midtrans_order_id, new.id::text);
  v_title text := 'Donasi berhasil';
  v_body text;
begin
  if coalesce(new.kategori, '') <> 'donasi' then
    return new;
  end if;

  if coalesce(new.status::text, '') <> 'success' then
    return new;
  end if;

  if tg_op = 'UPDATE'
     and coalesce(old.status::text, '') = coalesce(new.status::text, '')
     and coalesce(old.status_transaksi::text, '') = coalesce(new.status_transaksi::text, '') then
    return new;
  end if;

  if new.wali_id is null then
    return new;
  end if;

  v_body :=
    'Alhamdulillah, donasi/infaq sebesar Rp ' ||
    public.format_rupiah(v_amount) ||
    ' telah berhasil kami terima. Syukran wa jazakumullah khairan.';

  if not exists (
    select 1
    from public.notification_queue q
    where q.user_id = new.wali_id
      and q.event_type = 'payment.donation.success'
      and q.reference_id = v_reference_id
  ) then
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
    ) values (
      new.wali_id,
      v_title,
      v_body,
      jsonb_build_object(
        'type', 'donasi_success',
        'transaction_id', new.id,
        'order_id', new.midtrans_order_id,
        'amount', v_amount,
        'payment_method', new.metode_pembayaran,
        'status', new.status::text,
        'automatic', true
      ),
      'transaksi_keuangan',
      'payment.donation.success',
      'normal',
      'push',
      v_reference_id,
      now()
    );
  end if;

  return new;
end;
$$;

drop trigger if exists tr_notify_tagihan_payment_success on public.tagihan_santri;
create trigger tr_notify_tagihan_payment_success
after update of status on public.tagihan_santri
for each row
when (old.status is distinct from new.status and new.status = 'LUNAS')
execute function public.tr_notify_tagihan();

drop trigger if exists tr_notify_donasi_success_insert on public.transaksi_keuangan;
create trigger tr_notify_donasi_success_insert
after insert on public.transaksi_keuangan
for each row
when (new.kategori = 'donasi' and new.status = 'success')
execute function public.tr_notify_donasi_success();

drop trigger if exists tr_notify_donasi_success_update on public.transaksi_keuangan;
create trigger tr_notify_donasi_success_update
after update of status, status_transaksi on public.transaksi_keuangan
for each row
when (new.kategori = 'donasi' and new.status = 'success')
execute function public.tr_notify_donasi_success();

create index if not exists idx_notification_queue_payment_success_dedupe
  on public.notification_queue (event_type, reference_id, user_id)
  where event_type in ('payment.donation.success', 'payment.tagihan.success');

revoke all on function public.tr_notify_donasi_success() from public, anon, authenticated;
grant execute on function public.tr_notify_donasi_success() to service_role;
