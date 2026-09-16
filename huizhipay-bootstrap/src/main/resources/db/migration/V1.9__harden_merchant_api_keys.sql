alter table t_merchant_api_key add column if not exists environment varchar(8) not null default 'TEST';
alter table t_merchant_api_key add column if not exists status varchar(16) not null default 'ACTIVE';
alter table t_merchant_api_key add column if not exists key_name varchar(64) not null default 'Default Test Key';
alter table t_merchant_api_key add column if not exists scopes varchar(512) not null default 'payments:read payments:write';
alter table t_merchant_api_key add column if not exists created_by varchar(320);
alter table t_merchant_api_key add column if not exists disabled_by varchar(320);
alter table t_merchant_api_key add column if not exists activated_at timestamp;
alter table t_merchant_api_key add column if not exists last_used_at timestamp;
alter table t_merchant_api_key add column if not exists expires_at timestamp;
alter table t_merchant_api_key add column if not exists revocation_reason varchar(256);

update t_merchant_api_key
set status = case when enabled then 'ACTIVE' else 'REVOKED' end,
    activated_at = case when enabled then coalesce(activated_at, created_at) else activated_at end,
    created_by = coalesce(created_by, 'migration')
where created_by is null or activated_at is null;

with duplicates as (
  select id, row_number() over (partition by merchant_id, environment order by created_at desc, id desc) as rn
  from t_merchant_api_key where enabled = true
)
update t_merchant_api_key k
set enabled = false, status = 'REVOKED', disabled_at = current_timestamp,
    disabled_by = 'migration', revocation_reason = 'duplicate active key cleanup'
from duplicates d where k.id = d.id and d.rn > 1;

create unique index if not exists uk_merchant_api_key_active
  on t_merchant_api_key(merchant_id, environment) where enabled = true;
create index if not exists idx_merchant_api_key_lookup
  on t_merchant_api_key(key_hash, environment, enabled);

create table if not exists t_merchant_api_key_audit (
  id bigserial primary key,
  key_id bigint,
  merchant_id varchar(32) not null,
  environment varchar(8) not null,
  action varchar(32) not null,
  actor varchar(320) not null,
  detail varchar(512),
  created_at timestamp not null default current_timestamp
);
create index if not exists idx_api_key_audit_merchant_time
  on t_merchant_api_key_audit(merchant_id, created_at desc);
