---
title: HuizhiPay Sandbox 最小支付闭环功能需求表
status: Draft
version: v1
date: 2026-08-10
tags:
---


### 该PRD用于跑通一笔 Sandbox 模拟订单：

`商户创建订单 → 买家打开 HPP → 模拟成功/失败 → 7:93 记账 → 商户查看订单和事件日志`

固定规则：

- 只支持 USD。
- 只使用现有 `MOCK_CARD_GATEWAY`。
- 费率固定为 7%，商户净额为 93%。
- 不接真实银行卡、3DS2、Stellar、Polygon 或银行结算。
- Webhook 只记录事件，不发送 HTTP 请求。

## 最小功能需求

| ID | 功能 | 输入/显示 | 验收标准 |
|---|---|---|---|
| F-01 | 测试商户登录 | `email`, `password`, `totpCode?` | 登录后进入商户后台 |
| F-02 | 创建订单 | `merchantOrderNo`, `amount`, `currency=USD`, `returnUrl`；Header `Idempotency-Key` | 重复请求只创建一个订单，返回 HPP URL |
| F-03 | 加载 HPP | URL 中的 `checkoutToken` | 买家无需登录；显示商户名、金额和 USD |
| F-04 | 模拟支付 | `scenario=SUCCESS/FAILED` | 订单只产生一个最终结果 |
| F-05 | 结果页面 | 订单号、成功/失败状态、失败码、返回按钮 | 页面刷新后结果不变 |
| F-06 | 7:93 记账 | 订单号、商户、金额、USD | 同一订单只记账一次；分录合计为 0 |
| F-07 | 事件日志 | `payment.succeeded` 或 `payment.failed` | 每个订单只有一条最终事件 |
| F-08 | 商户订单详情 | 订单状态、gross、fee、net、事件 | 一页看完该订单的完整结果 |

## 复用仓库字段
现有字段

| 现有表 | 复用字段 |
|---|---|
| `t_user` | `id`, `email`, `password`, `status` |
| `t_merchant` | `merchant_id`, `owner_user_id`, `company_name` |
| `t_payment_order` | `order_no`, `merchant_id`, `amount`, `currency`, `channel`, `channel_trade_no`, `status`, `expire_at`, `created_at`, `updated_at` |
| `t_account` | `account_no`, `merchant_id`, `account_type`, `currency`, `balance`, `version` |
| `t_ledger_entry` | `merchant_id`, `account_no`, `amount`, `biz_type`, `biz_id`, `entry_status`, `created_at` |

## 只新增 6 个订单字段

| 字段 | 用途 | 规则 |
|---|---|---|
| `merchant_order_no` | 商户自己的订单号 | `(merchant_id, merchant_order_no)` 唯一 |
| `checkout_token_hash` | HPP 公网访问凭证 | 数据库只存 hash；明文 token 放在 HPP URL |
| `return_url` | 支付后返回商户 | 只允许测试域名 |
| `idempotency_key` | 防止重复创建订单 | `(merchant_id, idempotency_key)` 唯一 |
| `failure_code` | 显示模拟失败原因 | 本期只有 `MOCK_DECLINED` |
| `paid_at` | 记录成功时间 | 第一次成功时写入，之后不覆盖 |

## 新增一个最小事件表

```sql
create table t_webhook_event_log (
  id bigserial primary key,
  merchant_id varchar(32) not null,
  order_no varchar(64) not null,
  event_type varchar(64) not null,
  payload jsonb not null,
  created_at timestamp default current_timestamp,
  constraint uk_webhook_event_order_type unique (order_no, event_type)
);
```

账本增加唯一约束，防止重复记账：

```sql
unique (biz_id, account_no, biz_type)
```

## 只新增 4 个接口

| Method | Endpoint | 作用 |
|---|---|---|
| `POST` | `/api/v1/orders` | 商户创建 Sandbox 订单 |
| `GET` | `/api/v1/checkout/{token}` | 公网加载 HPP |
| `POST` | `/api/v1/checkout/{token}/pay` | 提交成功/失败模拟场景 |
| `GET` | `/api/v1/orders/{orderNo}` | 查询订单、账本和事件 |

## 固定记账公式

`fee = round(gross × 0.07, 3)`

`net = gross - fee`

| 账户 | 分录金额 |
|---|---:|
| 商户 `ASSET_AVAILABLE` | `+net` |
| 平台 `PLATFORM_INCOME` | `+fee` |
| 商户 `LIABILITY_CUSTODY` | `-gross` |

订单更新、三条账本分录和事件日志必须在同一数据库事务提交。

## 完成标准

- 同一个幂等键创建 3 次，只得到 1 个订单。
- 同一订单支付 3 次，只产生 1 次终态和 1 组账本分录。
- 成功订单满足 `gross = fee + net`，三条 USD 分录合计为 0。
- 失败订单不记账，只产生 `payment.failed` 事件。
- 商户订单详情能同时显示订单、分账和事件。
