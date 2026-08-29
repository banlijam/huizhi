---
title: HuizhiPay 前端实现检查与页面路由报告
status: v1.2
date: 2026-08-28
updated: 2026-08-29
tags:
  - powered-by/frontend-design
baseline_commit: 4c5c31317a243444fe77e42ae471fb87f97e4976
review_scope: D:\huizhi 当前工作区（本批改动提交前）
---

# HuizhiPay 前端实现检查与页面路由报告

## 1. 结论

当前前端已经覆盖 `pm dev/页面清单.md` 中的 **15 个规划地址**：公开页面 3 个、Checkout 2 个、Merchant Workspace 5 个、Developer Tools 5 个。核心 Dummy 闭环已经从“只接路由”推进到可运行状态：商户登录后创建订单，系统生成 `checkoutToken` 支付链接，买家查看商户名与 USD 金额、提交受控结果，并按订单的 `returnUrl` 返回商户网站。

本次重点复核的三项已落入实现：

1. **术语已统一为 `checkoutToken`**：前端、Dummy API、实体、数据库迁移和测试使用同一字段；内部 `orderNo` 仍保留为商户后台订单编号，但不再作为 Checkout 的公开查询参数。
2. **`returnUrl` 已接入**：建单请求保存返回地址；HPP 成功后倒计时跳转，失败状态提供“返回商户网站”链接。无效地址回退到 `/merchant`。
3. **登录门禁已接入**：`/merchant` 和 `/merchant/*`、`/developer/*` 私有占位页加载前都会调用 `GET /api/v1/auth/me`；未登录时跳转 `/login.html`，登录成功后统一进入 `/merchant`。

仍有一个参考流程没有完整实现：`isLoggedIn()` 只确认 `/auth/me` 返回有效用户，**尚未判断用户是否属于某个 Merchant 或具备对应角色/权限**。因此“已登录但未开通商户”的分支仍缺失。

## 2. 实现分层

| 层级 | 当前页面 | 结论 |
|---|---|---|
| 可运行 Dummy 功能 | `/merchant`、`/pay/?checkoutToken=...` | 已接真实 Dummy API 与数据库订单，可完成建单、查询、付款结果和返回链路 |
| 已接 API 的页面 | `/login.html` | 登录、注册、TOTP、忘记密码 UI 已存在；登录后进入 `/merchant` |
| 公开入口 | `/` | 已实现产品入口，链接到 Dashboard 登录与公开文档 |
| 最小占位 | `/docs`、`/checkout/widget`、4 个 Merchant 子页、5 个 Developer Tools 页面 | 地址和跳转可用，明确标记 `PLACEHOLDER · NOT LIVE`，不提交业务数据 |
| 内部原型 | `/demo` | Checkout、Merchant、Developer 视觉原型与状态演示，不属于公开产品导航 |
| 历史页面 | `src/views/*.html` | 旧 iframe Dashboard 页面仍在源码中，但未接入当前公开主路由 |

## 3. 路由逐项检查

### 3.1 公开页面

| 地址 | 实现文件 | 状态 | 备注 |
|---|---|---|---|
| `/` | `src/home.html` | 已实现 | 构建时覆盖发布包根 `index.html`，不再把 Merchant Dashboard 暴露为公开首页 |
| `/login`、`/login.html` | `src/login.html` | 已实现 | 统一登录入口；成功后使用绝对地址 `/merchant` |
| `/docs` | `src/docs.html` | 占位 | 无需登录；包含返回首页和登录 Dashboard 的入口 |

兼容入口：`/merchant/login` 通过服务器 302 或静态 `login-redirect.html` 跳转 `/login.html`；不提供 `/developer/login`。

### 3.2 买家 Checkout

| 地址 | 状态 | 与参考图一致性 |
|---|---|---|
| `/pay/?checkoutToken=...` | 可运行 Dummy HPP | 使用 `checkoutToken` 加载 `merchantName + amount + currency`；不显示后台导航；无需登录 |
| 同一 HPP 的成功状态 | 已实现 | 写入 `SUCCESS`，显示结果，倒计时后跳转订单 `returnUrl` |
| 同一 HPP 的失败状态 | 已实现 | 写入 `FAILED`，保持终态并提供返回商户网站链接；不在同一订单上重新支付 |
| 3DS2 | 未实现 | 仅在 `/demo` 有视觉原型，不属于真实支付链路 |
| `/checkout/widget` | 占位 | 仅验证组件示例地址，未提供 SDK、内嵌表单或结果区 |

Checkout 合同目前为：

- 创建订单：`POST /api/v1/orders`，请求可带 `amount`、`currency`、`returnUrl`。
- 加载订单：`GET /api/v1/orders/{checkoutToken}`。
- 写入结果：`POST /api/v1/orders/{checkoutToken}/result`。
- 返回字段包含 `checkoutToken`、`merchantName`、内部 `orderNo`、金额、币种、状态、`returnUrl` 和 `paymentUrl`。
- 数据库新增 `checkout_token` 唯一列和 `return_url` 列，迁移文件为 `V1.2__add_checkout_token_and_return_url.sql`。

### 3.3 Merchant Workspace

| 地址 | 状态 | 登录门禁 |
|---|---|---|
| `/merchant` | 可运行 Dummy Dashboard | 已接入 |
| `/merchant/onboarding` | 占位 | 已接入 |
| `/merchant/ledger` | 占位 | 已接入 |
| `/merchant/risk` | 占位 | 已接入 |
| `/merchant/wallet` | 占位 | 已接入 |

`/merchant` 当前包含建单表单、币种选择、订单总数、分页列表、支付链接和状态结果。订单详情暂以同页卡片/列表信息呈现；真实 7:93 账本、清算倒计时、KYB、风控规则和钱包管理尚未接到当前工作区。

### 3.4 Developer Tools

| 地址 | 状态 | 登录门禁 |
|---|---|---|
| `/developer` | 占位 | 已接入 |
| `/developer/api-keys` | 占位 | 已接入 |
| `/developer/sandbox` | 占位 | 已接入 |
| `/developer/webhooks` | 占位 | 已接入 |
| `/developer/logs` | 占位 | 已接入 |

Developer Tools 复用 Merchant Workspace 的登录状态，并提供返回 `/merchant` 和打开公开 `/docs` 的链接。旧地址 `/developer/docs` 只作为兼容跳转，目标为公开 `/docs`，不计为私有工具页面。

## 4. 对照 `页面流转图 1.md`

| 参考要求 | 当前实现 | 判断 |
|---|---|---|
| 买家通过 `checkoutToken` 打开 HPP | 前端和后端均已改用 `checkoutToken` | 一致 |
| HPP 显示商户名、金额和 USD | Dummy API 返回固定测试商户名；页面显示币种与金额 | 一致（Dummy 范围） |
| 成功/失败后返回商户 `returnUrl` | 成功自动跳转；失败提供返回链接 | 基本一致 |
| 私有页面未登录跳统一登录 | `/merchant` 与所有私有占位页均调用 `isLoggedIn()` | 一致 |
| 登录后调用 `/auth/me` | `isLoggedIn()` 调用 `GET /api/v1/auth/me` | 一致 |
| 判断是否属于 Merchant | 尚未校验 Merchant 归属或角色 | **缺失** |
| Developer 与 Merchant 共用工作区 | 共用登录与占位工作区，并可互相返回 | 一致 |
| `/docs` 公开 | 无登录门禁 | 一致 |
| SUCCESS、FAILED 为终态 | 后端只允许 `PENDING` 写入一次结果 | 一致 |

## 5. 源码与开发入口

### 5.1 前端

| 用途 | 权威文件 |
|---|---|
| 公开首页 | `D:\huizhi\huizhipay-frontend\src\home.html` |
| Merchant Dashboard 与内部 Demo | `D:\huizhi\huizhipay-frontend\src\index.html` |
| HPP | `D:\huizhi\huizhipay-frontend\src\pay\index.html` |
| 私有占位工作区与门禁 | `D:\huizhi\huizhipay-frontend\src\portal.html` |
| 统一登录 | `D:\huizhi\huizhipay-frontend\src\login.html` |
| 认证与通用 API | `D:\huizhi\huizhipay-frontend\src\js\api.js` |
| 公开文档占位 | `D:\huizhi\huizhipay-frontend\src\docs.html` |
| Widget 占位 | `D:\huizhi\huizhipay-frontend\src\checkout-placeholder.html` |
| 本地路由与 API 转发 | `D:\huizhi\huizhipay-frontend\server.js` |
| 静态构建与发布路由 | `D:\huizhi\huizhipay-frontend\scripts\build.cjs` |
| 集成测试 | `D:\huizhi\huizhipay-frontend\tests\frontend-build-api-proxy-integration.test.cjs` |

路由修改必须同步维护 `src`、`server.js`、`scripts/build.cjs` 和测试；`dist` 是自动生成产物，不直接编辑。

### 5.2 Dummy 后端

| 用途 | 权威文件 |
|---|---|
| 建单、查询、分页与支付结果 | `D:\huizhi\huizhipay-acquiring\src\main\java\com\huizhipay\acquiring\controller\DummyPaymentController.java` |
| 订单实体 | `D:\huizhi\huizhipay-acquiring\src\main\java\com\huizhipay\acquiring\entity\PaymentOrder.java` |
| Mapper | `D:\huizhi\huizhipay-acquiring\src\main\java\com\huizhipay\acquiring\mapper\PaymentOrderMapper.java` |
| 初始表结构 | `D:\huizhi\huizhipay-bootstrap\src\main\resources\db\migration\V1.0__init.sql` |
| Checkout 合同迁移 | `D:\huizhi\huizhipay-bootstrap\src\main\resources\db\migration\V1.2__add_checkout_token_and_return_url.sql` |

### 5.3 需求与设计

| 用途 | 文件 |
|---|---|
| 当前页面清单 | `C:\Users\Rowen\Desktop\OBS\HEAP\pay支付项目\pm dev\页面清单.md` |
| 当前页面流转图 | `C:\Users\Rowen\Desktop\OBS\HEAP\pay支付项目\pm dev\页面流转图 1.md` |
| Merchant 设计规范 | `D:\huizhi\.dev\v1-DesignSystem-MerchantDashboard-HuizhiPay-DarkPaymentOps-ImplementationGuide-20260829.md` |
| Merchant CSS Tokens | `D:\huizhi\.dev\v1-DesignTokens-MerchantDashboard-HuizhiPay-DarkPaymentOps-20260829.css` |
| 本报告 | `D:\huizhi\.dev\v1-前端实现检查报告-HuizhiPay-三端页面路由-占位页开发-20260828.md` |

## 6. 本批变更摘要

- 分离公开首页、Merchant Workspace、Developer Tools、公开文档和 Checkout 的路由语义。
- 删除已被统一工作区取代的 `src/developer.html`。
- 将 HPP 的旧外置 `pay.js`/`style.css` 收敛为当前自包含页面，避免两套实现漂移。
- 新增统一占位工作区、公开文档、Widget 和兼容跳转页面。
- 为 `/merchant` 及私有占位页接入登录门禁。
- 将 Checkout 查询参数和 API 路径统一为 `checkoutToken`，补充 `merchantName` 与 `returnUrl`。
- 新增数据库迁移，并扩展静态构建、开发服务器和跨层合同测试。

## 7. 验证结果

在 `D:\huizhi\huizhipay-frontend` 执行 `npm.cmd test`，当前自动化测试共 **3 项**：

1. 构建产物正确分离公开入口、Merchant、Developer Tools 和 Checkout。
2. 规划路由可访问，`/api/v1` 的方法、查询参数和请求体可转发到后端。
3. `checkoutToken`、`returnUrl`、数据库字段与登录门禁保持跨层一致。

浏览器合同检查结果：未登录访问 `/merchant` 会跳转 `/login.html`；旧 `orderNo` 参数不能加载订单；有效 `checkoutToken` 可显示 Dummy 商户名和 USD 金额，并使用订单 `returnUrl`。

限制：本机没有可用 Maven/Maven Wrapper，因此本次可验证前端构建与静态合同，**未完成 Java 后端编译和数据库迁移实跑**。

## 8. 主要缺口与风险

1. **Merchant 归属未校验**：认证门禁只判断“有用户”，没有实现参考图中的 Merchant membership/role 分支。
2. **Dummy 商户名仍是固定常量**：尚未从真实 Merchant 模型读取。
3. **业务占位仍多**：Ledger、KYB、Risk、Wallet、API Keys、Sandbox、Webhooks 和 API Logs 都没有真实后端闭环。
4. **两代前端并存**：当前自包含页面和 `src/views/*.html` 旧 iframe 页面可能继续漂移。
5. **单文件体积偏大**：`src/index.html` 同时承载 Merchant 与内部 Demo，继续扩展前应拆分组件与数据层。
6. **构建不完全可重复**：`build.cjs` 下载 `lucide@latest`，应固定版本。
7. **测试边界有限**：已有路由/转发/合同测试，但没有完整 DOM 点击链、移动端截图回归和可访问性检查。

## 9. 推荐下一步

最小优先级建议：先补 `/auth/me` 返回的 Merchant 归属与角色校验，再把 `/merchant/ledger` 接到当前 Dummy 订单结果，完成“建单 → Checkout → 终态 → 7:93 账本”的可测试闭环。Developer Tools 和 Embedded Widget 在对应后端能力存在前继续保持明确占位。
