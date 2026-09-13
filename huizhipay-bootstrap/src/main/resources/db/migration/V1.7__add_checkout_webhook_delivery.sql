create table t_transfi_checkout_webhook (
  id bigserial primary key,
  event_id varchar(128) not null unique,
  payload_sha256 char(64) not null,
  event_type varchar(64),
  channel_order_id varchar(128),
  platform_order_no varchar(64),
  merchant_order_no varchar(64),
  amount numeric(18,3),
  currency varchar(8),
  channel_status varchar(64),
  status varchar(32) not null,
  diagnostic varchar(256),
  received_at timestamp not null default current_timestamp,
  processed_at timestamp,
  updated_at timestamp not null default current_timestamp
);
create index idx_transfi_checkout_webhook_status on t_transfi_checkout_webhook(status, updated_at);

create table t_merchant_webhook_config (
  id bigserial primary key,
  merchant_id varchar(32) not null unique,
  endpoint_url varchar(512) not null,
  secret_ciphertext text not null,
  enabled boolean not null default true,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table t_merchant_webhook_delivery (
  id bigserial primary key,
  event_id varchar(128) not null unique,
  merchant_id varchar(32) not null,
  order_no varchar(64),
  event_type varchar(64) not null,
  payload_json jsonb not null,
  endpoint_url varchar(512) not null,
  status varchar(24) not null default 'PENDING',
  attempt_count int not null default 0,
  next_attempt_at timestamp not null default current_timestamp,
  lease_until timestamp,
  last_error varchar(256),
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);
create index idx_webhook_delivery_due on t_merchant_webhook_delivery(status, next_attempt_at, lease_until);
create index idx_webhook_delivery_merchant on t_merchant_webhook_delivery(merchant_id, created_at desc);

create table t_merchant_webhook_attempt (
  id bigserial primary key,
  delivery_id bigint not null references t_merchant_webhook_delivery(id),
  attempt_no int not null,
  response_status int,
  outcome varchar(24) not null,
  error_message varchar(256),
  attempted_at timestamp not null default current_timestamp,
  constraint uk_webhook_attempt unique(delivery_id, attempt_no)
);
create index idx_webhook_attempt_delivery on t_merchant_webhook_attempt(delivery_id, attempted_at desc);
