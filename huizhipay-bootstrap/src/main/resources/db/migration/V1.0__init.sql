-- =====================================================================
-- 绘智 HuizhiPay 初始化脚本（开发库，可清空重建）
-- 约定：
--   1. 数据库 PostgreSQL，所有时间列使用 TIMESTAMP（存储 UTC）
--   2. 所有金额列 numeric(18,3)，统一主币单位（元/美元），支持3位小数，不再使用“分”
--   3. 商户业务键 merchant_id 为 varchar，便于跨表关联
-- =====================================================================

-- 用户主表
create table t_user (
  id bigserial primary key,
  email varchar(128) not null unique,
  password varchar(256) not null,
  nickname varchar(64),
  email_verified boolean default false,
  totp_secret varchar(64),
  totp_enabled boolean default false,
  status smallint default 1,  -- 1启用，0禁用
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp
);

-- 邮箱验证令牌表
create table t_email_verification_token (
  id bigserial primary key,
  user_id bigint not null,
  token varchar(128) not null unique,
  type varchar(32) not null,  -- REGISTER 注册激活，RESET_PASSWORD 重置密码
  expiry_date TIMESTAMP not null,
  used boolean default false,
  created_at TIMESTAMP default current_timestamp
);
create index idx_email_verification_token_user_id on t_email_verification_token(user_id);

-- 商户主体表（KYB）
create table t_merchant (
  id bigserial primary key,
  merchant_id varchar(32) not null unique,                -- 业务键，如 M-20260808-xxxx
  owner_user_id bigint not null,                          -- 关联 t_user.id（管理账号）
  company_name varchar(128) not null,
  country varchar(64),
  license_no varchar(64),
  license_file_url varchar(256),
  legal_rep varchar(64),
  id_no varchar(64),
  settlement_pref varchar(16) default 'CRYPTO',            -- 结算偏好：CRYPTO / FIAT
  kyb_status varchar(16) default 'DRAFT',                  -- DRAFT / PENDING / APPROVED / REJECTED
  current_step smallint default 1,
  submitted_at TIMESTAMP,
  reviewed_at TIMESTAMP,
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp
);
create index idx_merchant_owner_user_id on t_merchant(owner_user_id);

-- 商户团队成员表
create table t_merchant_team (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  email varchar(128) not null,
  role varchar(32) not null,                              -- ADMIN / ANALYST / READONLY
  status varchar(16) default 'PENDING',                  -- ACCEPTED / PENDING
  sent_on TIMESTAMP default current_timestamp,
  created_at TIMESTAMP default current_timestamp
);
create index idx_merchant_team_merchant_id on t_merchant_team(merchant_id);

-- 商户结算钱包绑定表
create table t_merchant_wallet (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  wallet_type varchar(16) not null,                       -- metamask / stellar
  network varchar(32) not null,                           -- Polygon / Stellar
  address varchar(128) not null,
  bound_at TIMESTAMP default current_timestamp,
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp,
  constraint uk_merchant_wallet_merchant unique (merchant_id)
);

-- 商户风控规则开关表
create table t_risk_rule (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  rule_id varchar(32) not null,                           -- STRICT_MODE / BLOCK_PREPAID / FORCE_US_3DS / KYT_SCREENING / BLOCK_HIGH_RISK_REGION
  enabled boolean default false,
  category varchar(16) default 'NORMAL',                  -- MASTER / NORMAL
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp,
  constraint uk_risk_rule_merchant_rule unique (merchant_id, rule_id)
);
create index idx_risk_rule_merchant_id on t_risk_rule(merchant_id);

-- T+1 清算计划表（供清算倒计时）
create table t_settlement_schedule (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  settlement_date date not null,                          -- 清算日（T+1）
  expected_at TIMESTAMP not null,                       -- 预计到账时间（UTC）
  gross_amount numeric(18,3) default 0,                   -- 总流水
  fee_amount numeric(18,3) default 0,                     -- 绘智服务费（7%）
  net_amount numeric(18,3) default 0,                     -- 商户净收益（93%）
  status varchar(16) default 'PENDING',                   -- PENDING / SETTLED
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp
);
create index idx_settlement_schedule_merchant_id on t_settlement_schedule(merchant_id);

-- 收单支付订单表
create table t_payment_order (
  id bigserial primary key,
  order_no varchar(64) not null unique,
  merchant_id varchar(32) not null,
  amount numeric(18,3) not null,                          -- 主币单位（元/美元）
  currency varchar(8) default 'USD',
  channel varchar(32),                                    -- AIRWALLEX / WECHAT / ALIPAY
  fingerprint varchar(128),
  channel_trade_no varchar(128),                          -- 存 Airwallex 的 payment_intent_id
  status varchar(16) default 'PENDING',                   -- PENDING / SUCCESS / FAILED / CLOSED
  client_secret varchar(256),
  expire_at TIMESTAMP,
  remark varchar(256),
  version int default 0,
  deleted int default 0,
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp
);
create index idx_payment_order_merchant_id on t_payment_order(merchant_id);
create index idx_payment_order_status on t_payment_order(status);

-- 账户表
create table t_account (
  id bigserial primary key,
  account_no varchar(64) not null unique,
  merchant_id varchar(32) not null,
  account_type varchar(32) not null,                      -- ASSET_AVAILABLE / LIABILITY_CUSTODY
  currency varchar(8) default 'USD',
  balance numeric(18,3) default 0,
  version int default 0,
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp
);
create index idx_account_merchant_id on t_account(merchant_id);

-- 账本流水表
create table t_ledger_entry (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  account_no varchar(64) not null,
  amount numeric(18,3) not null,                          -- 正负金额：正=入账，负=扣减
  balance_before numeric(18,3),
  balance_after numeric(18,3),
  biz_type varchar(32),                                   -- PAYMENT / SETTLEMENT / FEE / REFUND ...
  biz_id varchar(64),
  channel varchar(32),
  external_order_id varchar(128),
  entry_status varchar(16) default 'SETTLED',             -- SETTLED / PENDING
  remark varchar(256),
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp
);
create index idx_ledger_entry_merchant_id on t_ledger_entry(merchant_id);
create index idx_ledger_entry_biz_id on t_ledger_entry(biz_id);

-- API 查询日志表
create table t_query_log (
  id bigserial primary key,
  query_no varchar(64) not null unique,
  merchant_id varchar(32) not null,
  product_id varchar(32),
  cost_amount numeric(18,3) default 0,                    -- 主币单位，存储正数
  query_params text,
  third_party_response text,
  status varchar(16),                                     -- SUCCESS / FAIL / REFUNDED
  error_message text,
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp
);
create index idx_query_log_merchant_id on t_query_log(merchant_id);

-- 渠道充值交易表
create table t_channel_recharge_tx (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  channel varchar(32),
  external_order_id varchar(128),
  amount numeric(18,3) default 0,
  currency varchar(8) default 'USD',
  channel_status varchar(32),
  raw_callback_payload jsonb,
  created_at TIMESTAMP default current_timestamp,
  updated_at TIMESTAMP default current_timestamp
);
create index idx_channel_recharge_tx_merchant_id on t_channel_recharge_tx(merchant_id);
