---
title: HuizhiPay Dummy MVP 前端
status: MVP
tags:
  - powered-by/frontend-design
  - powered-by/webapp-testing
---

# HuizhiPay Dummy MVP 前端

当前公开版本只实现一条可验收链路：商户创建 Dummy 订单，付款方模拟成功或失败，商户后台查看最终结果。不会发生真实扣款。

## 页面入口

| 地址 | 用途 |
|---|---|
| `/` 或 `/login` | 公开品牌首页与商户登录合并入口 |
| `/merchant` | 登录后的商户工作区：创建 Dummy 订单、查看订单状态和结果 |
| `/pay/?checkoutToken=...` | 买家付款页面：根据 Checkout Token 加载商户名、金额和订单状态 |
| `/developer` | 登录后商户工作区内的开发者工具 |
| `/demo` 或 `/?demo=1` | 团队内部界面原型；切换器、状态选择器和样例数据只在这里出现 |

## 本地运行

```bash
npm install
npm run dev
```

默认访问 `http://localhost:3000/`。前端会把 `/api/v1` 请求转发到 `http://localhost:8080`；可以通过 `BACKEND_API` 修改目标地址。

## Dummy 接口

| 方法 | 地址 | 用途 |
|---|---|---|
| `POST` | `/api/v1/dummy/orders` | 创建订单；请求可带 `returnUrl`，返回 `checkoutToken` 和 HPP 地址 |
| `GET` | `/api/v1/dummy/orders` | 查询最近 50 笔订单 |
| `GET` | `/api/v1/dummy/orders/{checkoutToken}` | 买家按 Checkout Token 查询单笔订单 |
| `POST` | `/api/v1/dummy/orders/{checkoutToken}/result` | 按 Checkout Token 写入 `SUCCESS` 或 `FAILED` |

## 验证

```bash
npm test
```

测试会检查构建产物、公开路由、Demo 路由、付款页以及 API 方法、查询参数和请求体转发。
