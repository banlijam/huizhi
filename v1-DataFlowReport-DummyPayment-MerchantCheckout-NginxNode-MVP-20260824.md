---
title: HuizhiPay Dummy 支付数据流转报告
status: Verified
version: v1
date: 2026-08-24
tags:
  - powered-by/webapp-testing
  - powered-by/skill-improve
---

# HuizhiPay Dummy 支付数据流转报告

## 验收结论

当前实现已跑通最小闭环：商户创建 USD Dummy 订单，付款方模拟成功或失败，商户后台重新读取并展示最终状态、结果与 Dummy 交易号。整个过程不发生真实扣款。

## 数据流转图

```mermaid
flowchart LR
    A[商户后台<br/>/ 或 /merchant] -->|金额 + 固定 USD| B[POST /api/v1/dummy/orders]
    B --> C[前端 Node/Nginx<br/>转发 /api/v1]
    C --> D[DummyPaymentController]
    D -->|INSERT PENDING| E[(t_payment_order)]
    E -->|orderNo + paymentUrl| D
    D --> C --> A

    A -->|打开 paymentUrl| F[付款页<br/>/pay/?orderNo=...]
    F -->|GET 单笔订单| D
    F -->|SUCCESS 或 FAILED| G[POST /api/v1/dummy/orders/{orderNo}/result]
    G --> D
    D -->|仅 PENDING 可进入终态| E
    E -->|status + transactionId + result| F

    A -->|刷新/返回后台| H[GET /api/v1/dummy/orders]
    H --> D -->|最近 50 笔 DUMMY 订单| A
```

## 各阶段字段

| 阶段 | 输入 | 数据库变化 | 返回/展示 |
|---|---|---|---|
| 创建订单 | `amount > 0`、`currency=USD` | 新增 `t_payment_order`；`channel=DUMMY`、`status=PENDING` | `orderNo`、`paymentUrl` |
| 加载付款页 | URL 中的 `orderNo` | 无 | 金额、USD、当前状态、交易号 |
| 模拟成功 | `result=SUCCESS` | `status=SUCCESS`，生成 `DUMMY_TXN_*`，写入成功说明 | 成功状态与交易号 |
| 模拟失败 | `result=FAILED` | `status=FAILED`，生成 `DUMMY_TXN_*`，写入失败说明 | 失败状态与交易号 |
| 后台查询 | 无 | 无 | 最近 50 笔订单及 PENDING/SUCCESS/FAILED 统计 |

## 状态规则

```text
创建订单 -> PENDING -> SUCCESS
                  \-> FAILED
```

- 只接受 `SUCCESS` 或 `FAILED`；其他值返回业务错误，不再默认判定为成功。
- 订单进入 `SUCCESS` 或 `FAILED` 后，再次提交结果只返回原订单，不覆盖已有终态或交易号。
- 本期只支持 USD；前端币种只读，后端同时拒绝非 USD 请求。

## 页面与部署路由

| 路由 | 正式行为 | Node 预览 | Nginx 静态产物 |
|---|---|---:|---:|
| `/`、`/merchant`、`/merchant/` | Dummy 商户后台 | 已验证 | 已生成目录入口 |
| `/pay`、`/pay/` | 响应式付款页 | 已验证 | 已保留目录入口 |
| `/demo`、`/demo/` | 内部原型和状态切换器 | 已验证 | 已生成目录入口 |
| `/developer`、`/developer/` | 本周暂缓说明 | 已验证 | 已生成目录入口 |

## 本次发现并修复的问题

1. Node 预览路由与 Nginx 静态构建产物不一致：构建阶段现为 `/merchant`、`/demo`、`/developer` 生成目录入口。
2. 尾斜杠路由可能 404：服务器现在统一解析并归一化 URL pathname。
3. 任意结果字符串会被当成成功：后端现在仅接受 `SUCCESS/FAILED`。
4. 已完成订单可以被改写：后端现在只允许 `PENDING` 进入一次终态。
5. 前端可选 EUR/HKD，但最小 MVP 定义为 USD：前后端现统一为 USD-only。

## 验证证据

- `npm test`：构建与路由/API 代理测试 2/2 通过。
- 真实 Chrome 无头验收（内存模拟后端，接口契约与 Dummy 控制器一致）：创建 USD 25.50 并模拟成功；创建 USD 12.00 并模拟失败；返回后台显示总数 2、成功 1、失败 1。
- Chrome 同时验证：正式入口隐藏悬浮岛，`/demo/` 显示悬浮岛，390px 宽度使用移动付款原型，`/developer/` 显示暂缓页，无控制台错误。
- 当前机器没有 Maven，因此 Java 控制器编译和 PostgreSQL 实库联调尚未在本机执行；不能将上述 Chrome 结果视为真实数据库验收。

## 当前明确不在数据流中的能力

本次 Dummy MVP 没有调用真实收单网关、`MOCK_CARD_GATEWAY`、3DS2、账本分录、7:93 分账、Webhook 事件、清结算或真实 API Key。这些不能从当前订单成功/失败状态推断为已完成。

## 防回归规则

- 路由拆分后必须同时测试开发服务器地址和 `dist` 静态目录入口，且包含有/无尾斜杠两种访问形式。
- 支付结果属于终态数据：只允许白名单结果值，并保证第一次终态写入后不可覆盖。
- Demo 页面和正式业务页面可以共享视觉组件，但不能依赖不同的请求字段、币种范围或状态含义。
