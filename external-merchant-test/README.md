# HuizhiPay External Merchant Test

独立的本地测试商户网站。它拥有自己的 SQLite 订单库，只通过 HuizhiPay Test API 和签名 Webhook 与支付平台通信，不共享商户后台 Cookie 或 PostgreSQL。

## 运行要求

- Node.js 22.5 或更高版本
- 已启动的 HuizhiPay 后端
- 为测试商户签发的 `hzp_test_...` API Key

## 本地启动

1. 复制 `.env.example` 为 `.env`，填写 `HUIZHIPAY_TEST_API_KEY`。
2. 运行 `start.ps1`、`start.cmd` 或 `npm start`。
3. 打开 `http://127.0.0.1:14330`。

本地测试允许 `http://127.0.0.1` 和 `http://localhost` 作为 `PUBLIC_ORIGIN`；任何非 loopback 地址必须使用 HTTPS。

运行数据保存在 `data/orders.sqlite`，不会包含在发布包或 Git 中。

## 测试与打包

```powershell
npm test
npm run package
```

打包产物位于 `dist/`。该目录可整体复制到另一台装有 Node.js 22.5+ 的测试机，编辑 `.env` 后运行启动脚本。

## HTTP 边界

- `POST /api/orders`：按固定商品目录创建商户订单。
- `GET /api/orders/{accessToken}`：使用随机买家令牌查询订单并同步平台状态。
- `POST /webhooks/huizhipay`：验签、幂等保存平台通知并更新本地订单。

浏览器不会接触 `HUIZHIPAY_TEST_API_KEY` 或 Webhook secret。
