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
  created_at timestamp without time zone default current_timestamp,
  updated_at timestamp without time zone default current_timestamp
);

-- 邮箱验证令牌表
create table t_email_verification_token (
  id bigserial primary key,
  user_id bigint not null,
  token varchar(128) not null unique,
  type varchar(32) not null,  -- REGISTER 注册激活，RESET_PASSWORD 重置密码
  expiry_date timestamp without time zone not null,
  used boolean default false,
  created_at timestamp without time zone default current_timestamp,
  constraint fk_email_verification_token_user foreign key (user_id) references t_user(id) on delete cascade
);

create index idx_email_verification_token_user_id on t_email_verification_token(user_id);