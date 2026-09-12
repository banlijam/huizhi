-- One user can own only one merchant. The service already reuses the owned
-- merchant on resubmission; this index is the final concurrent-write guard.
create unique index if not exists uk_merchant_owner_user_id
  on t_merchant(owner_user_id);

-- Minimal audit trail for the controlled sandbox KYB review script.
create table if not exists t_kyb_review_audit (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  owner_email varchar(128) not null,
  reviewer varchar(128) not null,
  previous_status varchar(16) not null,
  decision varchar(16) not null,
  source varchar(32) not null,
  reviewed_at timestamp not null default current_timestamp
);

create index if not exists idx_kyb_review_audit_merchant_id
  on t_kyb_review_audit(merchant_id);
