<div align="center">

# Huizhi Settlement Bridge (HSB)

**GDPR-Compliant Cross-Border B2B & SME Settlement Middleware**

[![GitHub stars](https://img.shields.io/github/stars/huizhipay/huizhipay?style=social)](https://github.com/banlijam/huizhi)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

[English](README.md) · [中文](README.zh-CN.md)

</div>

---

## 🌟 Overview

**Huizhi Settlement Bridge (HSB)** is an API-driven, non-custodial financial infrastructure that optimizes cross-border
B2B settlement across high-friction corridors.

Traditional mechanisms suffer from extended clearing cycles and high costs for SMEs. HSB solves this as a headless
middleware routing engine, bridging compliant fiat gateways with distributed ledger clearing networks.

### Key Highlights

- **Non-Custodial Architecture**: Zero counterparty risk — funds flow directly between sender and receiver
- **Multi-Corridor Clearing**: Supports emerging corridors (e.g., Africa, Latin America) where traditional rails are
  limited
- **GDPR Compliance**: Built-in data minimization and consent management middleware
- **Sub-Second Settlement**: Leverages Stellar Network for instant, low-cost cross-border transactions
- **SEPs & Standards**: Implements SEP-06 and SEP-24 for secure deposit/withdrawal workflows

---

## 🏗️ Architecture

### Technical Architecture Diagram

<div align="center">
  <img src="docs/hsb-architecture.jpg" alt="HSB Technical Architecture" width="100%" />
</div>

### Layer Breakdown

| Layer                   | Description                                                                                |
|-------------------------|--------------------------------------------------------------------------------------------|
| **Layer 1**             | SME Input & Order Management — Product catalog, data minimization middleware, API router   |
| **Layer 2**             | Middleware & Router — Multi-channel aggregator, data protection, virtual ledger management |
| **Blockchain Clearing** | Stellar Path Payment Engine — Optimal cross-currency path matching, SEP protocols          |
| **Payout & Settlement** | Cross-border data transfer, licensed stellar anchors, merchant bank accounts               |

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **PostgreSQL 15+**
- **Node.js 20+** (for frontend)
- **Stellar Account** (testnet for development)

### Backend Setup

```bash
# Clone the repository
git clone https://github.com/huizhipay/huizhipay.git
cd huizhipay

# Configure database
# Edit huizhipay-bootstrap/src/main/resources/application.yml

# Build the project
mvn clean install -DskipTests

# Run the application
cd huizhipay-bootstrap
mvn spring-boot:run
```

### Frontend Setup

```bash
# Navigate to frontend directory
cd huizhipay-frontend

# Install dependencies
npm install

# Start development server
npm start
```

Visit `http://localhost:3000` to access the dashboard.

---

## 📁 Project Structure

```
huizhipay/
├── huizhipay-common/          # Common utilities, models, configurations
├── huizhipay-extensions/      # Manifold extensions (StringExtension, BigDecimal)
├── huizhipay-user/            # User service (Auth, Registration, TOTP)
├── huizhipay-acquiring/       # Payment acquiring service
├── huizhipay-ledger/          # Ledger and accounting service
├── huizhipay-risk/            # Risk management service
├── huizhipay-settlement/      # Settlement processing service
├── huizhipay-bootstrap/       # Application bootstrap and configuration
├── huizhipay-frontend/        # Frontend dashboard (Vanilla JS)
└── docs/                      # Documentation and diagrams
```

---

## 🔧 Core Modules

| Module           | Technology               | Description                                              |
|------------------|--------------------------|----------------------------------------------------------|
| **User Service** | Spring Security + JWT    | Authentication, TOTP, email verification, password reset |
| **Acquiring**    | Stellar SDK              | Crypto payments, multi-channel aggregation               |
| **Ledger**       | Double-entry bookkeeping | Virtual accounts, balances, fee management               |
| **Risk**         | Rule engine              | Transaction screening, anomaly detection                 |
| **Settlement**   | SEP-06/SEP-24            | Path payments, anchor integration                        |

---

## 🔐 Security

- **JWT + HttpOnly Cookies**: Secure token management
- **GDPR Compliance**: Data minimization, consent management
- **Encryption at Rest & in Transit**: AES-256, TLS 1.3
- **TOTP Two-Factor Authentication**: RFC 6238 compliant
- **Input/Output Validation**: Strict schema validation

---

## 🌍 API Documentation

Start the application and navigate to:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

### Key Endpoints

| Method | Endpoint                       | Description                 |
|--------|--------------------------------|-----------------------------|
| `POST` | `/api/v1/auth/register`        | Register new user           |
| `POST` | `/api/v1/auth/login`           | Login with email + password |
| `POST` | `/api/v1/auth/logout`          | Logout and clear session    |
| `GET`  | `/api/v1/auth/me`              | Get current user info       |
| `POST` | `/api/v1/auth/forgot-password` | Request password reset      |
| `GET`  | `/api/v1/auth/verify-email`    | Verify email with token     |

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Support

If you find HSB useful, please consider:

- ⭐ Starring the repository on GitHub
- 🔄 Sharing with your network
- 💬 Providing feedback and suggestions

For enterprise support or partnerships, contact us at **service@huizhipay.com**
