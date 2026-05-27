<div align="center">

# 绘智结算桥 (HSB)

**符合 GDPR 的跨境 B2B 与中小企业结算中间件**

[![GitHub stars](https://img.shields.io/github/stars/huizhipay/huizhipay?style=social)](https://github.com/huizhipay/huizhipay)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

[English](README.md) · [中文](README.zh-CN.md)

</div>

---

## 🌟 项目概述

**绘智结算桥 (HSB)** 是一个 API 驱动的非托管金融基础设施，旨在优化高摩擦走廊的跨境 B2B 结算。

传统结算机制存在清算周期长、中小企业成本高等问题。HSB 作为无头中间件路由引擎解决了这一问题，连接合规的法币网关与分布式账本清算网络。

### 核心亮点

- **非托管架构**：零交易对手风险 — 资金在发送方和接收方之间直接流转
- **多走廊清算**：支持新兴走廊（如非洲、拉丁美洲），传统轨道有限
- **GDPR 合规**：内置数据最小化与同意管理中间件
- **亚秒级结算**：利用 Stellar 网络实现即时、低成本的跨境交易
- **SEP 标准协议**：实现 SEP-06 和 SEP-24 协议，提供安全的存取款工作流

---

## 🏗️ 技术架构

### 技术架构图

<div align="center">
  <img src="docs/hsb-architecture.jpg" alt="HSB 技术架构图" width="100%" />
</div>

### 分层说明

| 层级             | 描述                                                          |
|------------------|---------------------------------------------------------------|
| **第一层**       | 中小企业输入与订单管理 — 产品目录、数据最小化中间件、API 路由 |
| **第二层**       | 中间件与路由器 — 多渠道聚合器、数据保护、虚拟账本管理         |
| **区块链清算层** | Stellar 路径支付引擎 — 最优跨币种路径匹配、SEP 协议           |
| **支付与结算层** | 跨境数据传输、持牌 Stellar 锚点、商户银行账户                 |

---

## 🚀 快速开始

### 环境要求

- **Java 21+**
- **Maven 3.8+**
- **PostgreSQL 15+**
- **Node.js 20+**（前端使用）
- **Stellar 账户**（开发可使用测试网）

### 后端配置

```bash
# 克隆仓库
git clone https://github.com/huizhipay/huizhipay.git
cd huizhipay

# 配置数据库
# 编辑 huizhipay-bootstrap/src/main/resources/application.yml

# 构建项目
mvn clean install -DskipTests

# 运行应用
cd huizhipay-bootstrap
mvn spring-boot:run
```

### 前端配置

```bash
# 进入前端目录
cd huizhipay-frontend

# 安装依赖
npm install

# 启动开发服务器
npm start
```

访问 `http://localhost:3000` 进入控制台。

---

## 📁 项目结构

```
huizhipay/
├── huizhipay-common/          # 通用工具、模型、配置
├── huizhipay-extensions/      # Manifold 扩展（StringExtension、BigDecimal）
├── huizhipay-user/            # 用户服务（认证、注册、TOTP）
├── huizhipay-acquiring/       # 支付收单服务
├── huizhipay-ledger/          # 账本与记账服务
├── huizhipay-risk/            # 风险管理服务
├── huizhipay-settlement/      # 结算处理服务
├── huizhipay-bootstrap/       # 应用启动与配置
├── huizhipay-frontend/        # 前端控制台（原生 JS）
└── docs/                      # 文档与图表
```

---

## 🔧 核心模块

| 模块         | 技术栈                | 描述                           |
|--------------|-----------------------|--------------------------------|
| **用户服务** | Spring Security + JWT | 认证、TOTP、邮箱验证、密码重置 |
| **收单服务** | Stellar SDK           | 加密支付、多渠道聚合           |
| **账本服务** | 复式记账法            | 虚拟账户、余额、手续费管理     |
| **风控服务** | 规则引擎              | 交易筛查、异常检测             |
| **结算服务** | SEP-06/SEP-24         | 路径支付、锚点集成             |

---

## 🔐 安全机制

- **JWT + HttpOnly Cookie**：安全的令牌管理
- **GDPR 合规**：数据最小化、同意管理
- **静态与传输加密**：AES-256、TLS 1.3
- **TOTP 双因素认证**：符合 RFC 6238 标准
- **输入输出验证**：严格的模式验证

---

## 🌍 API 文档

启动应用后访问：

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API 文档: `http://localhost:8080/api-docs`

### 主要接口

| 方法   | 接口路径                       | 描述             |
|--------|--------------------------------|------------------|
| `POST` | `/api/v1/auth/register`        | 注册新用户       |
| `POST` | `/api/v1/auth/login`           | 邮箱密码登录     |
| `POST` | `/api/v1/auth/logout`          | 登出并清除会话   |
| `GET`  | `/api/v1/auth/me`              | 获取当前用户信息 |
| `POST` | `/api/v1/auth/forgot-password` | 请求密码重置     |
| `GET`  | `/api/v1/auth/verify-email`    | 通过令牌验证邮箱 |

---

## 📄 开源协议

本项目采用 Apache License 2.0 协议 — 请参阅 [LICENSE](LICENSE) 文件了解详情。

---

## 🙏 技术支持

如果您觉得 HSB 有用，欢迎：

- ⭐ 在 GitHub 上给本项目点星
- 🔄 分享给您的网络
- 💬 提供反馈与建议

企业级支持或合作联系： **service@huizhipay.com**
