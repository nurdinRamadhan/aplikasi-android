-- Improve donation success notification copy and route it to any logged-in donor
-- captured on the transaction, not only wali accounts.

create or replace function public.tr_notify_donasi_success()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_amount numeric := coalesce(new.jumlah, 0);
  v_reference_id text := coalesce(new.midtrans_order_id, new.id::text);
  v_recipient_id uuid := coalesce(new.wali_id, new.admin_pencatat_id);
  v_title text := 'Donasi Berhasil Diterima';
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

  if v_recipient_id is null then
    return new;
  end if;

  v_body :=
    'Alhamdulillah, donasi/infaq Bapak/Ibu sebesar Rp ' ||
    public.format_rupiah(v_amount) ||
    ' telah berhasil kami terima. Semoga menjadi amal jariyah dan membawa keberkahan. Jazakumullah khairan.';

  if not exists (
    select 1
    from public.notification_queue q
    where q.user_id = v_recipient_id
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
      v_recipient_id,
      v_title,
      v_body,
      jsonb_build_object(
        'type', 'donasi_success',
        'transaction_id', new.id,
        'order_id', new.midtrans_order_id,
        'amount', v_amount,
        'payment_method', new.metode_pembayaran,
        'status', new.status::text,
        'donor_user_id', v_recipient_id,
        'recipient_source', case
          when new.wali_id is not null then 'wali_id'
          when new.admin_pencatat_id is not null then 'admin_pencatat_id'
          else 'unknown'
        end,
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

revoke all on function public.tr_notify_donasi_success() from public, anon, authenticated;
grant execute on function public.tr_notify_donasi_success() to service_role;
