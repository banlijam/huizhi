-- =============================================================
-- V1.1__seed_test_data.sql
-- 绘智支付 (HuiZhiPay) 开发库测试数据
-- 仅用于开发环境，可随时清空重建。
-- 约定：
--   所有时间为 UTC (TIMESTAMP)
--   所有金额为主币单位，numeric(18,3)
--   枚举值为 Java Enum.name()：大写，如 DRAFT / CRYPTO
-- =============================================================

-- -------------------------------------------------------------
-- 1. 用户（密码为 BCrypt 加密的 "123456"，下同）
-- -------------------------------------------------------------
insert into t_user (email, password, nickname, email_verified, totp_enabled, totp_secret, status, created_at,
                    updated_at)
values ('admin@huizhipay.org',
        '$2a$10$2DajysHKBjjzuUKtc8je4O5Qh5lEoLUd5ZmsaIReyVZ9PT35wk3Hm',
        '绘智管理员', true, false, null, 1,
        now() at time zone 'utc', now() at time zone 'utc'),

       ('merchant@huizhipay.org',
        '$2a$10$2DajysHKBjjzuUKtc8je4O5Qh5lEoLUd5ZmsaIReyVZ9PT35wk3Hm',
        '绘智演示商户', true, false, null, 1,
        now() at time zone 'utc', now() at time zone 'utc'),

       ('alice@acme.test',
        '$2a$10$2DajysHKBjjzuUKtc8je4O5Qh5lEoLUd5ZmsaIReyVZ9PT35wk3Hm',
        'Alice ACME', true, false, null, 1,
        now() at time zone 'utc', now() at time zone 'utc'),

       ('bob@foobar.test',
        '$2a$10$2DajysHKBjjzuUKtc8je4O5Qh5lEoLUd5ZmsaIReyVZ9PT35wk3Hm',
        'Bob Foobar', false, false, null, 1,
        now() at time zone 'utc', now() at time zone 'utc');

-- -------------------------------------------------------------
-- 2. 商户主体（KYB）
-- -------------------------------------------------------------
insert into t_merchant (merchant_id, owner_user_id, company_name, country, license_no, license_file_url,
                        legal_rep, id_no, settlement_pref, kyb_status, current_step, submitted_at, reviewed_at,
                        created_at, updated_at)
values
    -- 平台演示商户（已通过审核）
    ('M-20260801-DEMOHZ', 2,
     'HuiZhiPay Demo Merchant Pte. Ltd.', 'Singapore',
     'SG-2026-AC-8392', '/uploads/sg-license-8392.pdf',
     'Zhang Wei', 'S1234567D',
     'CRYPTO', 'APPROVED', 4,
     (now() - interval '20 days') at time zone 'utc',
     (now() - interval '18 days') at time zone 'utc',
     (now() - interval '30 days') at time zone 'utc',
     now() at time zone 'utc'),

    -- ACME 演示商户（审核中）
    ('M-20260806-ACME01', 3,
     'ACME Technology Inc.', 'United States',
     'US-DE-8876543210', '/uploads/us-acme-license.pdf',
     'Alice Carter', '000-00-0001',
     'CRYPTO', 'PENDING', 4,
     (now() - interval '2 days') at time zone 'utc',
     null,
     (now() - interval '5 days') at time zone 'utc',
     now() at time zone 'utc'),

    -- Foobar 商户（草稿，尚未提交）
    ('M-20260807-FOOBAR', 4,
     'Foobar Consulting Ltd.', 'Hong Kong',
     'HK-BR-66554433', null,
     'Bob Tan', 'Z12345678',
     'FIAT', 'DRAFT', 3,
     null, null,
     (now() - interval '1 day') at time zone 'utc',
     now() at time zone 'utc');

-- -------------------------------------------------------------
-- 3. 商户团队
-- -------------------------------------------------------------
insert into t_merchant_team (merchant_id, email, role, status, sent_on, created_at)
values ('M-20260801-DEMOHZ', 'ops@huizhipay.org', 'ADMIN', 'ACCEPTED', (now() - interval '25 days') at time zone 'utc',
        (now() - interval '25 days') at time zone 'utc'),
       ('M-20260801-DEMOHZ', 'risk@huizhipay.org', 'ANALYST', 'PENDING', (now() - interval '2 days') at time zone 'utc',
        (now() - interval '2 days') at time zone 'utc'),
       ('M-20260801-DEMOHZ', 'fin@huizhipay.org', 'READONLY', 'PENDING', (now() - interval '1 day') at time zone 'utc',
        (now() - interval '1 day') at time zone 'utc'),
       ('M-20260806-ACME01', 'alice+2@acme.test', 'ADMIN', 'ACCEPTED', (now() - interval '4 days') at time zone 'utc',
        (now() - interval '4 days') at time zone 'utc');

-- -------------------------------------------------------------
-- 4. 商户结算钱包
-- -------------------------------------------------------------
insert into t_merchant_wallet (merchant_id, wallet_type, network, address, created_at, updated_at)
values ('M-20260801-DEMOHZ', 'METAMASK', 'POLYGON', '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1',
        now() at time zone 'utc', now() at time zone 'utc'),
       ('M-20260806-ACME01', 'STELLAR', 'TESTNET', 'GAI7B4GOPJ336ZYXW2VTL55S5MFTFLL3XQ5KNO7I7A4ZQXHV3C3DEMO',
        now() at time zone 'utc', now() at time zone 'utc');

-- -------------------------------------------------------------
-- 5. 风控规则（种子默认值）
-- -------------------------------------------------------------
insert into t_risk_rule (merchant_id, rule_id, enabled, category, created_at, updated_at)
values
    -- DEMOHZ：严格模式打开
    ('M-20260801-DEMOHZ', 'STRICT_MODE', true, 'MASTER', now() at time zone 'utc', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'BLOCK_PREPAID', true, 'NORMAL', now() at time zone 'utc', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'FORCE_US_3DS', true, 'NORMAL', now() at time zone 'utc', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'KYT_SCREENING', true, 'NORMAL', now() at time zone 'utc', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'BLOCK_HIGH_RISK_REGION', false, 'NORMAL', now() at time zone 'utc',
     now() at time zone 'utc'),
    -- ACME：严格模式关闭
    ('M-20260806-ACME01', 'STRICT_MODE', false, 'MASTER', now() at time zone 'utc', now() at time zone 'utc'),
    ('M-20260806-ACME01', 'BLOCK_PREPAID', true, 'NORMAL', now() at time zone 'utc', now() at time zone 'utc'),
    ('M-20260806-ACME01', 'FORCE_US_3DS', false, 'NORMAL', now() at time zone 'utc', now() at time zone 'utc'),
    ('M-20260806-ACME01', 'KYT_SCREENING', true, 'NORMAL', now() at time zone 'utc', now() at time zone 'utc'),
    ('M-20260806-ACME01', 'BLOCK_HIGH_RISK_REGION', false, 'NORMAL', now() at time zone 'utc',
     now() at time zone 'utc');

-- -------------------------------------------------------------
-- 6. 清算计划（T+1）
-- -------------------------------------------------------------
insert into t_settlement_schedule (merchant_id, settlement_date, gross_amount, fee_amount, net_amount, expected_at,
                                   status, created_at, updated_at)
values ('M-20260801-DEMOHZ', (now() at time zone 'utc')::date, 12500.500, 875.035, 11625.465,
        ((now() at time zone 'utc') + interval '1 day')::date::timestamp at time zone 'utc',
        'PENDING', now() at time zone 'utc', now() at time zone 'utc'),
       ('M-20260801-DEMOHZ', ((now() - interval '1 day') at time zone 'utc')::date, 9820.000, 687.400, 9132.600,
        (now() at time zone 'utc')::date::timestamp at time zone 'utc',
        'SETTLED', now() at time zone 'utc', now() at time zone 'utc'),
       ('M-20260806-ACME01', (now() at time zone 'utc')::date, 3200.750, 224.053, 2976.697,
        ((now() at time zone 'utc') + interval '1 day')::date::timestamp at time zone 'utc',
        'PENDING', now() at time zone 'utc', now() at time zone 'utc');

-- -------------------------------------------------------------
-- 7. 账户（初始余额为 0，通过交易累积）
-- -------------------------------------------------------------
insert into t_account (account_no, merchant_id, account_type, currency, balance, created_at, updated_at, version)
values
    -- DEMOHZ
    ('ACC-M-DEMOHZ-AVAIL', 'M-20260801-DEMOHZ', 'ASSET_AVAILABLE', 'USD', 0.000, now() at time zone 'utc', now() at time zone 'utc', 0),
    ('ACC-M-DEMOHZ-LIAB', 'M-20260801-DEMOHZ', 'LIABILITY_CUSTODY', 'USD', 0.000, now() at time zone 'utc', now() at time zone 'utc', 0),
    -- ACME
    ('ACC-M-ACME01-AVAIL', 'M-20260806-ACME01', 'ASSET_AVAILABLE', 'USD', 0.000, now() at time zone 'utc', now() at time zone 'utc', 0),
    ('ACC-M-ACME01-LIAB', 'M-20260806-ACME01', 'LIABILITY_CUSTODY', 'USD', 0.000, now() at time zone 'utc', now() at time zone 'utc', 0),
    -- 平台账户
    ('ACC-PLATFORM-INCOME', '__PLATFORM__', 'PLATFORM_INCOME', 'USD', 0.000, now() at time zone 'utc', now() at time zone 'utc', 0),
    ('ACC-PLATFORM-COST', '__PLATFORM__', 'PLATFORM_COST', 'USD', 0.000, now() at time zone 'utc', now() at time zone 'utc', 0);

-- -------------------------------------------------------------
-- 8. 支付订单
-- -------------------------------------------------------------
insert into t_payment_order (order_no, merchant_id, amount, channel, status, created_at, updated_at)
values ('P-260808-0001', 'M-20260801-DEMOHZ', 1200.000, 'stripe_us', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260808-0002', 'M-20260801-DEMOHZ', 850.500, 'stripe_sg', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260808-0003', 'M-20260801-DEMOHZ', 3200.000, 'adyen_hk', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260808-0004', 'M-20260801-DEMOHZ', 250.000, 'stripe_us', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260808-0005', 'M-20260801-DEMOHZ', 980.000, 'stripe_sg', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260808-0006', 'M-20260801-DEMOHZ', 455.000, 'adyen_hk', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260808-0007', 'M-20260801-DEMOHZ', 168.300, 'stripe_us', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260808-0008', 'M-20260806-ACME01', 500.000, 'stripe_us', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260808-0009', 'M-20260806-ACME01', 1200.000, 'stripe_sg', 'SUCCESS', now() at time zone 'utc', now() at time zone 'utc'),
       ('P-260807-1001', 'M-20260801-DEMOHZ', 2100.000, 'stripe_us', 'SUCCESS',
        (now() - interval '1 day') at time zone 'utc', now() at time zone 'utc');

-- -------------------------------------------------------------
-- 9. 分账流水（复式记账：商户净得 + 平台手续费 + 托管负债，借贷必平衡）
--    每笔订单 3 条流水，借贷总和 = 0
-- -------------------------------------------------------------
insert into t_ledger_entry (merchant_id, account_no, amount, balance_before, balance_after, biz_type, biz_id, channel,
                            external_order_id, entry_status, remark, created_at)
values
    -- 订单 0001: 1200 → 商户净得 1116 + 平台费 84 - 托管 1200
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-AVAIL', 1116.000, 0.000, 1116.000, 'PAYMENT', 'P-260808-0001', 'stripe_us', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 84.000, 0.000, 84.000, 'PAYMENT', 'P-260808-0001', 'stripe_us', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-LIAB', -1200.000, 0.000, -1200.000, 'PAYMENT', 'P-260808-0001', 'stripe_us', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 0002: 850.500 → 商户净得 790.965 + 平台费 59.535 - 托管 850.500
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-AVAIL', 790.965, 1116.000, 1906.965, 'PAYMENT', 'P-260808-0002', 'stripe_sg', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 59.535, 84.000, 143.535, 'PAYMENT', 'P-260808-0002', 'stripe_sg', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-LIAB', -850.500, -1200.000, -2050.500, 'PAYMENT', 'P-260808-0002', 'stripe_sg', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 0003: 3200 → 商户净得 2976 + 平台费 224 - 托管 3200
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-AVAIL', 2976.000, 1906.965, 4882.965, 'PAYMENT', 'P-260808-0003', 'adyen_hk', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 224.000, 143.535, 367.535, 'PAYMENT', 'P-260808-0003', 'adyen_hk', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-LIAB', -3200.000, -2050.500, -5250.500, 'PAYMENT', 'P-260808-0003', 'adyen_hk', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 0004: 250 → 商户净得 232.500 + 平台费 17.500 - 托管 250
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-AVAIL', 232.500, 4882.965, 5115.465, 'PAYMENT', 'P-260808-0004', 'stripe_us', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 17.500, 367.535, 385.035, 'PAYMENT', 'P-260808-0004', 'stripe_us', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-LIAB', -250.000, -5250.500, -5500.500, 'PAYMENT', 'P-260808-0004', 'stripe_us', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 0005: 980 → 商户净得 911.400 + 平台费 68.600 - 托管 980
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-AVAIL', 911.400, 5115.465, 6026.865, 'PAYMENT', 'P-260808-0005', 'stripe_sg', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 68.600, 385.035, 453.635, 'PAYMENT', 'P-260808-0005', 'stripe_sg', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-LIAB', -980.000, -5500.500, -6480.500, 'PAYMENT', 'P-260808-0005', 'stripe_sg', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 0006: 455 → 商户净得 423.150 + 平台费 31.850 - 托管 455
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-AVAIL', 423.150, 6026.865, 6450.015, 'PAYMENT', 'P-260808-0006', 'adyen_hk', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 31.850, 453.635, 485.485, 'PAYMENT', 'P-260808-0006', 'adyen_hk', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-LIAB', -455.000, -6480.500, -6935.500, 'PAYMENT', 'P-260808-0006', 'adyen_hk', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 0007: 168.300 → 商户净得 156.519 + 平台费 11.781 - 托管 168.300
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-AVAIL', 156.519, 6450.015, 6606.534, 'PAYMENT', 'P-260808-0007', 'stripe_us', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 11.781, 485.485, 497.266, 'PAYMENT', 'P-260808-0007', 'stripe_us', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-LIAB', -168.300, -6935.500, -7103.800, 'PAYMENT', 'P-260808-0007', 'stripe_us', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 0008 (ACME): 500 → 商户净得 465 + 平台费 35 - 托管 500
    ('M-20260806-ACME01', 'ACC-M-ACME01-AVAIL', 465.000, 0.000, 465.000, 'PAYMENT', 'P-260808-0008', 'stripe_us', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 35.000, 497.266, 532.266, 'PAYMENT', 'P-260808-0008', 'stripe_us', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260806-ACME01', 'ACC-M-ACME01-LIAB', -500.000, 0.000, -500.000, 'PAYMENT', 'P-260808-0008', 'stripe_us', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 0009 (ACME): 1200 → 商户净得 1116 + 平台费 84 - 托管 1200
    ('M-20260806-ACME01', 'ACC-M-ACME01-AVAIL', 1116.000, 465.000, 1581.000, 'PAYMENT', 'P-260808-0009', 'stripe_sg', null, 'SETTLED', '商户净得 93%', now() at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 84.000, 532.266, 616.266, 'PAYMENT', 'P-260808-0009', 'stripe_sg', null, 'SETTLED', '平台手续费 7%', now() at time zone 'utc'),
    ('M-20260806-ACME01', 'ACC-M-ACME01-LIAB', -1200.000, -500.000, -1700.000, 'PAYMENT', 'P-260808-0009', 'stripe_sg', null, 'SETTLED', '托管负债增加', now() at time zone 'utc'),

    -- 订单 1001 (昨日): 2100 → 商户净得 1953 + 平台费 147 - 托管 2100
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-AVAIL', 1953.000, 6606.534, 8559.534, 'PAYMENT', 'P-260807-1001', 'stripe_us', null, 'SETTLED', '商户净得 93%', (now() - interval '1 day') at time zone 'utc'),
    ('__PLATFORM__', 'ACC-PLATFORM-INCOME', 147.000, 616.266, 763.266, 'PAYMENT', 'P-260807-1001', 'stripe_us', null, 'SETTLED', '平台手续费 7%', (now() - interval '1 day') at time zone 'utc'),
    ('M-20260801-DEMOHZ', 'ACC-M-DEMOHZ-LIAB', -2100.000, -7103.800, -9203.800, 'PAYMENT', 'P-260807-1001', 'stripe_us', null, 'SETTLED', '托管负债增加', (now() - interval '1 day') at time zone 'utc');

-- -------------------------------------------------------------
-- 10. 最近 7 天销量（用于指挥中心图表 X 轴）
--    以生成的 order_id 前缀 P-26080X 匹配即可，额外再塞几笔昨日之前的
-- -------------------------------------------------------------
insert into t_payment_order (order_no, merchant_id, amount, channel, status, created_at, updated_at)
values ('P-260807-1002', 'M-20260801-DEMOHZ', 1800.000, 'stripe_sg', 'SUCCESS',
        (now() - interval '1 day') at time zone 'utc', now() at time zone 'utc'),
       ('P-260806-1003', 'M-20260801-DEMOHZ', 2200.000, 'adyen_hk', 'SUCCESS',
        (now() - interval '2 days') at time zone 'utc', now() at time zone 'utc'),
       ('P-260805-1004', 'M-20260801-DEMOHZ', 1500.000, 'stripe_us', 'SUCCESS',
        (now() - interval '3 days') at time zone 'utc', now() at time zone 'utc'),
       ('P-260804-1005', 'M-20260801-DEMOHZ', 980.000, 'stripe_us', 'SUCCESS',
        (now() - interval '4 days') at time zone 'utc', now() at time zone 'utc'),
       ('P-260803-1006', 'M-20260801-DEMOHZ', 760.000, 'stripe_sg', 'SUCCESS',
        (now() - interval '5 days') at time zone 'utc', now() at time zone 'utc'),
       ('P-260802-1007', 'M-20260801-DEMOHZ', 3100.000, 'adyen_hk', 'SUCCESS',
        (now() - interval '6 days') at time zone 'utc', now() at time zone 'utc');

-- -------------------------------------------------------------
-- 11. 查询日志（外部查询成本由平台承担，不再从商户扣费）
-- -------------------------------------------------------------
insert into t_query_log (query_no, merchant_id, product_id, cost_amount, query_params, third_party_response, status,
                         created_at)
values ('Q-260808-000001', 'M-20260801-DEMOHZ', 'KYC_BASIC', 0.800,
        '{"supplierId":"SUPPLIER-001","docNo":"S1234567D","name":"Zhang Wei"}',
        '{"score":88,"match":true}', 'SUCCESS', now() at time zone 'utc'),
       ('Q-260808-000002', 'M-20260801-DEMOHZ', 'KYT_TX', 0.800,
        '{"supplierId":"SUPPLIER-002","txHash":"0xabc...","network":"Polygon"}',
        '{"risk":"low"}', 'SUCCESS', now() at time zone 'utc'),
       ('Q-260808-000003', 'M-20260806-ACME01', 'KYC_BASIC', 0.800,
        '{"supplierId":"SUPPLIER-001","docNo":"000-00-0001","name":"Alice Carter"}',
        '{"score":71,"match":true}', 'SUCCESS', now() at time zone 'utc');

-- -------------------------------------------------------------
-- 12. 渠道充值交易（用于 acquiring 对账）
-- -------------------------------------------------------------
insert into t_channel_recharge_tx (merchant_id, channel, external_order_id, amount, currency, channel_status,
                                   raw_callback_payload, created_at, updated_at)
values ('M-20260801-DEMOHZ', 'stripe_us', 'CH-STRIPE-20260808-1001', 1200.000, 'USD', 'SUCCESS',
        '{
          "pi": "pi_xxx",
          "balance_transaction": "txn_xxx",
          "order_id": "P-260808-0001"
        }',
        now() at time zone 'utc', now() at time zone 'utc'),
       ('M-20260801-DEMOHZ', 'adyen_hk', 'CH-ADYEN-20260808-1002', 3200.000, 'HKD', 'SUCCESS',
        '{
          "pspReference": "881...",
          "order_id": "P-260808-0003"
        }',
        now() at time zone 'utc', now() at time zone 'utc');
