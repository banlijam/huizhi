# HuizhiPay 商户控制台

HuizhiPay 商户控制台是一个现代化的支付管理仪表盘，提供总览、保理与结算、集成管理、风控与团队等功能模块。

## 技术栈

- **前端**: HTML5 + Tailwind CSS + Vanilla JavaScript
- **后端**: Node.js (内置 HTTP 模块)
- **图表**: Chart.js
- **多语言**: 内置中英文支持

## 快速开始

### 方式一：使用 Node.js 服务器启动 (推荐)

```bash
# 启动服务器
node server.js

# 访问地址
http://localhost:3000
```

### 方式二：直接本地打开

直接用浏览器打开 `src/index.html` 文件即可使用，所有功能均有本地 mock 数据支持。

> 注意：直接打开时，API 请求会自动回退到本地 mock 数据，不会发起网络请求。

## 项目结构

```
huizhi-pay/
├── server.js              # Node.js HTTP 服务器
├── src/
│   ├── index.html         # 主页面 Shell
│   ├── styles.css         # 全局样式变量
│   ├── i18n/              # 语言配置文件
│   │   ├── zh.js          # 中文配置
│   │   └── en.js          # 英文配置
│   ├── js/                # 共享 JavaScript 模块
│   │   ├── i18n.js        # 多语言工具函数
│   │   ├── api.js         # API 调用封装（含 mock 数据）
│   │   ├── app.js         # 主页面逻辑
│   │   └── toast.js       # Toast 通知组件
│   └── views/             # 视图页面（iframe 加载）
│       ├── overview.html      # 总览
│       ├── factoring.html     # 保理与结算
│       ├── integrations.html  # 集成
│       └── risk.html          # 风控与团队
├── README.md              # 项目文档
└── package.json
```

## 功能模块

### 1. 总览 (Overview)
- API 调用统计
- 成功率指标
- 认证交易额
- 7 日 3DS 使用量图表
- 近期 3DS 交易日志

### 2. 保理与结算 (Factoring & Settlement)
- 拒付率监控
- 保理额度管理
- T+1 结算列表

### 3. 集成 (Integrations)
- Shopify 连接管理
- WooCommerce 连接管理
- 实时同步状态

### 4. 风控与团队 (Risk & Team)
- 风控规则配置
- 团队成员邀请管理
- 角色权限分配

## 后端接口文档

### 认证说明

所有接口需要在请求头中携带认证信息：

| Header | 说明 |
|--------|------|
| `Authorization` | Bearer Token，格式：`Bearer <token>` |

### 1. 用户信息

#### GET /api/v1/user/profile

获取当前用户信息和余额。

**请求参数**: 无

**响应示例**:

```json
{
  "balance": 12450.00,
  "email": "merchant@example.com",
  "company": "HuizhiPay"
}
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| balance | number | 可用余额（USD） |
| email | string | 用户邮箱 |
| company | string | 公司名称 |

---

### 2. 总览

#### GET /api/v1/overview/stats

获取总览页面统计数据。

**请求参数**: 无

**响应示例**:

```json
{
  "apiCalls": 4210,
  "apiCallsChange": 12.4,
  "successRate": 94.2,
  "successRateChange": 1.8,
  "authVolume": 86400,
  "authVolumeChange": 8.1,
  "chartData": {
    "labels": ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
    "requests": [482, 620, 545, 718, 680, 824, 756],
    "approved": [451, 578, 519, 664, 642, 771, 713]
  }
}
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| apiCalls | number | API 调用次数（过去 24 小时） |
| apiCallsChange | number | 较前一周期变化百分比 |
| successRate | number | 成功率（%） |
| successRateChange | number | 较前一周期变化百分比 |
| authVolume | number | 认证交易额（USD） |
| authVolumeChange | number | 较前一周期变化百分比 |
| chartData.labels | string[] | 图表 X 轴标签 |
| chartData.requests | number[] | 3DS 请求数 |
| chartData.approved | number[] | 认证通过数 |

---

### 3. 3DS 交易

#### GET /api/v1/3ds/transactions

获取最近的 3DS 交易日志。

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | number | 否 | 返回条数，默认 10 |

**响应示例**:

```json
[
  {
    "time": "14:32:08",
    "orderId": "HP-839201",
    "card": "•••• 4242",
    "provider": "Adyen",
    "status": "verified",
    "cavvEci": "AAABBI / 05"
  },
  {
    "time": "14:18:41",
    "orderId": "HP-839184",
    "card": "•••• 1881",
    "provider": "Stripe",
    "status": "pending",
    "cavvEci": "kB8F2x / 05"
  }
]
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| time | string | 交易时间 |
| orderId | string | 订单 ID |
| card | string | 脱敏卡号 |
| provider | string | 支付提供商 |
| status | string | 状态：`verified`（已验证）、`pending`（需审核）、`failed`（失败） |
| cavvEci | string | CAVV / ECI 值 |

---

### 4. 保理与结算

#### GET /api/v1/factoring/stats

获取保理统计数据。

**响应示例**:

```json
{
  "chargebackRate": 0.8,
  "chargebackRateChange": -0.1,
  "factoringLimit": 50000,
  "factoringUsed": 28200,
  "pendingNet": 37000,
  "pendingCount": 3
}
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| chargebackRate | number | 拒付率（%） |
| chargebackRateChange | number | 较前一周期变化 |
| factoringLimit | number | 保理额度（USD） |
| factoringUsed | number | 已使用额度（USD） |
| pendingNet | number | 待结算净额（USD） |
| pendingCount | number | 待结算笔数 |

#### GET /api/v1/factoring/settlements

获取 T+1 结算列表。

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | number | 否 | 返回条数，默认 10 |

**响应示例**:

```json
[
  {
    "id": "PO-20260723-01",
    "date": "2026-07-23",
    "channel": "visaNa",
    "gross": 18420.00,
    "fees": 326.14,
    "net": 18093.86,
    "status": "processing"
  }
]
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 结算 ID |
| date | string | 到账日期（YYYY-MM-DD） |
| channel | string | 通道标识：`visaNa`、`mastercardEu`、`localApac` |
| gross | number | 交易总额（USD） |
| fees | number | 费用（USD） |
| net | number | 预计净额（USD） |
| status | string | 状态：`processing`（处理中）、`locked`（已锁定）、`estimated`（预估） |

---

### 5. 集成

#### GET /api/v1/integrations

获取集成列表。

**响应示例**:

```json
[
  {
    "id": "shopify",
    "name": "Shopify",
    "description": "Sync orders, refunds, and 3DS verification status.",
    "status": "running",
    "version": "API v2026-07",
    "authType": "OAuth 2.0",
    "lastSync": "2026-07-23T14:30:00Z"
  },
  {
    "id": "woocommerce",
    "name": "WooCommerce",
    "description": "Receive real-time transaction events via secure webhooks.",
    "status": "disabled",
    "version": "API v9.8",
    "authType": "OAuth 2.0",
    "lastSync": null
  }
]
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 集成 ID |
| name | string | 集成名称 |
| description | string | 集成描述 |
| status | string | 状态：`running`（运行中）、`disabled`（未启用） |
| version | string | API 版本 |
| authType | string | 认证方式 |
| lastSync | string/null | 最近同步时间（ISO 8601） |

#### PUT /api/v1/integrations/{id}

启用/停用集成。

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string | 集成 ID |

**请求参数**:

```json
{
  "enabled": true
}
```

**响应示例**:

```json
{
  "id": "shopify",
  "enabled": true,
  "status": "running"
}
```

---

### 6. 风控

#### GET /api/v1/risk/rules

获取风控规则列表。

**响应示例**:

```json
[
  {
    "id": "blockPrepaid",
    "name": "Block Prepaid Cards",
    "description": "Block transactions from prepaid BINs.",
    "enabled": true
  },
  {
    "id": "forceUs3ds",
    "name": "Force 3DS for US BINs",
    "description": "Execute challenge flow for US-issued cards.",
    "enabled": true
  }
]
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 规则 ID |
| name | string | 规则名称 |
| description | string | 规则描述 |
| enabled | boolean | 是否启用 |

#### PUT /api/v1/risk/rules/{id}

启用/停用风控规则。

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string | 规则 ID |

**请求参数**:

```json
{
  "enabled": false
}
```

**响应示例**:

```json
{
  "id": "blockPrepaid",
  "enabled": false
}
```

---

### 7. 团队

#### GET /api/v1/team/members

获取团队成员列表。

**响应示例**:

```json
[
  {
    "email": "ops@huizhipay.org",
    "role": "admin",
    "sentOn": "2026-07-21",
    "status": "accepted"
  },
  {
    "email": "risk@huizhipay.org",
    "role": "analyst",
    "sentOn": "2026-07-22",
    "status": "pending"
  }
]
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| email | string | 成员邮箱 |
| role | string | 角色：`admin`（运营管理员）、`analyst`（风控分析员）、`readonly`（只读财务） |
| sentOn | string | 邀请发送日期 |
| status | string | 状态：`accepted`（已接受）、`pending`（待接受） |

#### POST /api/v1/team/invite

邀请团队成员。

**请求参数**:

```json
{
  "email": "newmember@example.com",
  "role": "analyst"
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邀请邮箱 |
| role | string | 是 | 角色 |

**响应示例**:

```json
{
  "success": true,
  "email": "newmember@example.com",
  "role": "analyst"
}
```

---

### 8. 充值

#### POST /api/v1/topup/invoice

生成充值账单。

**请求参数**:

```json
{
  "amount": 1000
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| amount | number | 是 | 充值金额（USDT），最低 10 USDT |

**响应示例 (成功)**:

```json
{
  "invoiceId": "INV-12345678",
  "amount": 1000,
  "network": "TRON (TRC20)",
  "address": "TUeJ9rYbX9qN9Qj7X8y7x6w5v4u3t2s1r"
}
```

**响应示例 (失败)**:

```json
{
  "error": "Minimum top-up amount is 10 USDT"
}
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| invoiceId | string | 账单 ID |
| amount | number | 充值金额 |
| network | string | 网络类型 |
| address | string | 充值地址 |

---

## 多语言支持

系统支持中英文两种语言：

### 切换语言

点击页面右上角的语言选择器即可切换语言。

### 语言持久化

语言设置会自动保存到浏览器的 localStorage 中，下次访问时会自动应用上次选择的语言。

### 当前语言标识

- `zh` - 中文
- `en` - English

## 后端接口对接说明

当后台接口可用时，需要修改以下部分以对接真实后端：

### 1. 修改 API 基础地址

修改 `src/js/api.js` 文件中的 `API_BASE` 变量：

```javascript
// 修改前（使用本地 mock 数据）
const API_BASE = '/api/v1';

// 修改后（对接真实后端）
const API_BASE = 'https://api.your-domain.com/api/v1';
```

### 2. 认证配置

确保后端支持 Bearer Token 认证，前端会自动在请求头中携带：

```
Authorization: Bearer <token>
```

### 3. 需要实现的后端接口

后端需要实现以下完整的 RESTful API 接口：

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 用户信息 | `/api/v1/user/profile` | GET | 获取用户信息和余额 |
| 总览 | `/api/v1/overview/stats` | GET | 获取总览统计数据 |
| 3DS 交易 | `/api/v1/3ds/transactions` | GET | 获取交易日志 |
| 保理 | `/api/v1/factoring/stats` | GET | 获取保理统计 |
| 结算 | `/api/v1/factoring/settlements` | GET | 获取结算列表 |
| 集成 | `/api/v1/integrations` | GET | 获取集成列表 |
| 集成 | `/api/v1/integrations/{id}` | PUT | 启用/停用集成 |
| 风控 | `/api/v1/risk/rules` | GET | 获取风控规则 |
| 风控 | `/api/v1/risk/rules/{id}` | PUT | 启用/停用规则 |
| 团队 | `/api/v1/team/members` | GET | 获取团队成员 |
| 团队 | `/api/v1/team/invite` | POST | 邀请成员 |
| 充值 | `/api/v1/topup/invoice` | POST | 生成充值账单 |

### 4. CORS 配置

后端需要配置 CORS 允许前端域名访问：

```
Access-Control-Allow-Origin: https://your-frontend-domain.com
Access-Control-Allow-Methods: GET, POST, PUT, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

### 5. 对接后的行为

- 所有页面数据将从后端获取，不再使用本地 mock 数据
- 用户操作（如启用集成、修改风控规则）会实时同步到后端
- Toast 通知提示操作结果
- 页面刷新后会重新从后端加载最新数据

---

## 配置说明

### 端口配置

服务器默认监听 `3000` 端口，可以通过环境变量 `PORT` 修改：

```bash
PORT=8080 node server.js
```

### 本地打开模式

当页面直接通过 `file://` 协议打开时：
- 所有 API 请求会自动回退到本地 mock 数据
- 无需启动服务器即可使用所有功能

## 浏览器兼容性

支持所有现代浏览器：
- Chrome (推荐)
- Firefox
- Safari
- Edge

## License

MIT