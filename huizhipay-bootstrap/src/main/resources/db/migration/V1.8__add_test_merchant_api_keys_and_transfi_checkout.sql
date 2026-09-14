-- Test-environment merchant credentials and TransFi Checkout order fields.
-- These belong to the normal HuizhiPay schema; TransFi alone remains Sandbox.
create table if not exists t_merchant_api_key (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  key_prefix varchar(24) not null,
  key_hash varchar(64) not null unique,
  enabled boolean not null default true,
  created_at timestamp not null default current_timestamp,
  disabled_at timestamp
);

create index if not exists idx_merchant_api_key_merchant
  on t_merchant_api_key(merchant_id, enabled);

alter table t_payment_order add column if not exists merchant_order_no varchar(64);
alter table t_payment_order add column if not exists payment_url varchar(2048);
alter table t_payment_order add column if not exists channel_status varchar(32);

create unique index if not exists uk_payment_order_merchant_external
  on t_payment_order(merchant_id, merchant_order_no)
  where merchant_order_no is not null and deleted = 0;
