create table t_payment_event_log (
  id bigserial primary key,
  order_no varchar(64) not null,
  merchant_id varchar(32) not null,
  event_type varchar(64) not null,
  transaction_id varchar(128),
  created_at timestamp default current_timestamp,
  constraint uk_payment_event_order_type unique (order_no, event_type)
);

create index idx_payment_event_merchant_id on t_payment_event_log(merchant_id);

create unique index uk_ledger_entry_business_account
  on t_ledger_entry(biz_type, biz_id, account_no);
