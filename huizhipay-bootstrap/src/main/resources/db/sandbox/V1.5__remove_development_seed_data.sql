-- V1.1 is part of the existing Flyway history and cannot be rewritten safely.
-- This migration is loaded only by the sandbox profile, leaving local/dev data
-- untouched while ensuring a newly-created sandbox starts without demo tenants.
delete from t_channel_recharge_tx where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR');
delete from t_query_log where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR');
delete from t_ledger_entry where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR', '__PLATFORM__');
delete from t_payment_order where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR');
delete from t_settlement_schedule where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR');
delete from t_risk_rule where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR');
delete from t_merchant_wallet where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR');
delete from t_merchant_team where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR');
delete from t_account where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR', '__PLATFORM__');
delete from t_merchant where merchant_id in ('M-20260801-DEMOHZ', 'M-20260806-ACME01', 'M-20260807-FOOBAR');
delete from t_user where email in ('admin@huizhipay.org', 'merchant@huizhipay.org', 'alice@acme.test', 'bob@foobar.test');
