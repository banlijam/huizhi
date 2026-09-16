create table if not exists t_merchant_redirect_origin (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  environment varchar(8) not null default 'TEST',
  origin varchar(512) not null,
  verified boolean not null default false,
  verification_token_hash varchar(64),
  created_by varchar(320) not null,
  verified_at timestamp,
  disabled_at timestamp,
  created_at timestamp not null default current_timestamp,
  unique(merchant_id, environment, origin)
);

create index if not exists idx_redirect_origin_lookup
  on t_merchant_redirect_origin(merchant_id, environment, verified)
  where disabled_at is null;
