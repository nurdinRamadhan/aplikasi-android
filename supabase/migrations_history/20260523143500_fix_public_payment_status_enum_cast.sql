create or replace function public.get_payment_status_public(p_order_id text)
returns table(status text)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_order_id text := nullif(trim(p_order_id), '');
  v_status text;
  v_tagihan_id text;
begin
  if v_order_id is null or length(v_order_id) > 160 then
    return query select 'PENDING'::text;
    return;
  end if;

  select
    case
      when lower(coalesce(t.status::text, '')) in ('success', 'settlement', 'capture', 'paid', 'lunas', 'posted')
        or lower(coalesce(t.status_transaksi::text, '')) in ('success', 'settlement', 'capture', 'paid', 'lunas', 'posted')
        then 'SUCCESS'
      when lower(coalesce(t.status::text, '')) in ('failed', 'failure', 'deny', 'denied', 'cancel', 'canceled', 'cancelled', 'expire', 'expired')
        or lower(coalesce(t.status_transaksi::text, '')) in ('failed', 'failure', 'deny', 'denied', 'cancel', 'canceled', 'cancelled', 'expire', 'expired')
        then 'FAILED'
      else 'PENDING'
    end
  into v_status
  from public.transaksi_keuangan t
  where t.midtrans_order_id = v_order_id
  order by t.created_at desc
  limit 1;

  if v_status is not null and v_status <> 'PENDING' then
    return query select v_status;
    return;
  end if;

  select
    case
      when lower(coalesce(w.status::text, '')) in ('success', 'settlement', 'capture', 'paid', 'lunas', 'posted')
        then 'SUCCESS'
      when lower(coalesce(w.status::text, '')) in ('failed', 'failure', 'deny', 'denied', 'cancel', 'canceled', 'cancelled', 'expire', 'expired')
        then 'FAILED'
      else 'PENDING'
    end
  into v_status
  from public.wallet_payment_intents w
  where w.midtrans_order_id = v_order_id
  order by w.updated_at desc
  limit 1;

  if v_status is not null and v_status <> 'PENDING' then
    return query select v_status;
    return;
  end if;

  v_tagihan_id := coalesce(nullif(split_part(v_order_id, '_', 1), ''), v_order_id);

  select
    case
      when ts.status = 'LUNAS' then 'SUCCESS'
      else 'PENDING'
    end
  into v_status
  from public.tagihan_santri ts
  where ts.id::text in (v_order_id, v_tagihan_id)
  limit 1;

  return query select coalesce(v_status, 'PENDING')::text;
end;
$$;

revoke all on function public.get_payment_status_public(text) from public;
grant execute on function public.get_payment_status_public(text) to anon, authenticated;
