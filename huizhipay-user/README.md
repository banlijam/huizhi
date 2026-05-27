# 绘智付 (HuiZhiPay) 用户认证接口文档

**基础路径**：`/api/v1/auth`

**请求头**：`Content-Type: application/json`

**鉴权**：除注册、登录、验证邮箱、忘记密码、重置密码外，其余接口需在 `Authorization` 头中携带 `Bearer <accessToken>`。

---

## 一、认证流程概览

### 1. 注册与邮箱验证

```text
[前端] 提交注册信息 → [后端] 发送验证邮件 → [用户] 点击邮件链接 → [后端] 激活账号
```

### 2. 登录（支持密码 + 可选的谷歌验证器）

```text
[前端] 提交邮箱+密码（可选totpCode） → [后端] 校验密码及邮箱激活状态
    ├─ 未开启 TOTP → 直接返回 JWT
    └─ 已开启 TOTP →
        ├─ 请求未带 totpCode → 返回 { totpRequired: true }
        └─ 请求带 totpCode → 校验 TOTP 后返回 JWT
```

### 3. 绑定谷歌验证器（需登录）

```text
[前端] 获取 TOTP 密钥和二维码 → [用户] 扫描二维码（如 Google Authenticator） → [前端] 提交 secret + 动态验证码 → [后端] 绑定并启用
```

### 4. 忘记密码

```text
[前端] 提交邮箱 → [后端] 发送重置密码邮件 → [用户] 点击链接跳转重置页面 → [前端] 提交新密码 + token → [后端] 重置成功
```

---

## 二、通用数据结构

### 统一响应体（Result\<T\>）

```json
{
  "code": 0,
  // 0 表示成功，其他为错误码
  "message": "success",
  "data": {
    ...
  }
  // 具体数据，可为 null
}
```

### 错误码参考（部分）

| code | 说明                      |
|------|---------------------------|
| 0    | 成功                      |
| 1001 | 邮箱或密码错误            |
| 1002 | 邮箱未验证                |
| 1003 | 账户被禁用                |
| 1004 | 验证码错误或过期          |
| 1005 | 无效或过期的链接          |
| 1006 | 邮箱已注册                |
| 1007 | 参数校验失败              |
| 1008 | 未授权（Token 无效/过期） |

---

## 三、接口详情

### 1. 用户注册

注册成功后，系统会向邮箱发送激活邮件（有效期 15 分钟）。

- **URL**：`/register`
- **Method**：`POST`

**请求体**：

```json
{
  "email": "user@example.com",
  // 必填，邮箱格式
  "password": "123456",
  // 必填，长度6~20
  "nickname": "张三"
  // 可选
}
```

**响应示例（成功）**：

```json
{
  "code": 0,
  "message": "注册成功，请前往邮箱激活",
  "data": null
}
```

**错误示例**：

```json
{
  "code": 1006,
  "message": "邮箱已被注册",
  "data": null
}
```

---

### 2. 邮箱验证（激活账号）

用户点击邮件中的链接，前端需将 token 作为查询参数访问该接口。

- **URL**：`/verify-email?token={token}`
- **Method**：`GET`

**响应示例（成功）**：

```json
{
  "code": 0,
  "message": "邮箱激活成功，请登录",
  "data": null
}
```

**错误**：返回 `1005` 无效或过期链接。

---

### 3. 用户登录

支持密码登录，若用户已绑定并开启 TOTP，则需二次验证。

- **URL**：`/login`
- **Method**：`POST`

**请求体**：

```json
{
  "email": "user@example.com",
  "password": "123456",
  "totpCode": 123456
  // 可选，当用户已开启 TOTP 时必填
}
```

**响应（成功）**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "totpRequired": false
  }
}
```

若用户已开启 TOTP 但请求未带 `totpCode`，则返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": null,
    "totpRequired": true
  }
}
```

> **提示**：前端应根据 `totpRequired` 提示用户输入动态码，并再次调用同一接口（带上 `totpCode`）。

**错误示例（密码错误）**：

```json
{
  "code": 1001,
  "message": "邮箱或密码错误",
  "data": null
}
```

---

### 4. 获取 TOTP 绑定信息（需登录）

登录后，用户可绑定 Google Authenticator。此接口返回密钥和二维码内容，用于前端生成二维码。

- **URL**：`/totp/setup`
- **Method**：`POST`
- **请求头**：`Authorization: Bearer <accessToken>`

**响应**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    // 密钥（Base32），需保存至确认接口
    "qrCodeUrl": "otpauth://totp/HuiZhiPay:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=HuiZhiPay"
  }
}
```

> **提示**：前端可利用 `qrCodeUrl` 生成二维码（如使用 qrcode.js 库）。

---

### 5. 确认绑定 TOTP（需登录）

用户扫描二维码后，在 Authenticator 中获取 6 位动态码，提交完成绑定。

- **URL**：`/totp/confirm`
- **Method**：`POST`
- **请求头**：`Authorization: Bearer <accessToken>`

**请求体**：

```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  // 来自上一步
  "code": 123456
  // 用户输入的6位数字
}
```

**响应（成功）**：

```json
{
  "code": 0,
  "message": "TOTP绑定成功",
  "data": null
}
```

**错误**：验证码错误返回 `1004`。

---

### 6. 忘记密码 – 发送重置邮件

向用户邮箱发送包含重置链接的邮件（链接带 token，有效期 15 分钟）。

- **URL**：`/forgot-password`
- **Method**：`POST`

**请求体**：

```json
{
  "email": "user@example.com"
}
```

**响应（成功）**：

```json
{
  "code": 0,
  "message": "重置密码邮件已发送，请查收",
  "data": null
}
```

> **注意**：无论邮箱是否存在，均返回相同提示（防止邮箱枚举）。

---

### 7. 重置密码（通过 token）

用户点击邮件中的链接，进入重置页面，前端收集新密码后调用此接口。

- **URL**：`/reset-password`
- **Method**：`POST`

**请求体**：

```json
{
  "token": "email-reset-token-uuid",
  // 邮件中的 token
  "newPassword": "new123456"
}
```

**响应（成功）**：

```json
{
  "code": 0,
  "message": "密码重置成功，请登录",
  "data": null
}
```

**错误**：token 无效或过期返回 `1005`。

---

### 8. 获取当前登录用户信息（需登录）

用于前端展示用户基本信息。

- **URL**：`/me`
- **Method**：`GET`
- **请求头**：`Authorization: Bearer <accessToken>`

**响应**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10001,
    "email": "user@example.com",
    "nickname": "张三",
    "emailVerified": true,
    "totpEnabled": true,
    "status": 1
  }
}
```

---

## 四、前端实现建议

### 登录流程（TOTP 已开启场景）

```javascript
// 第一步：尝试登录（不带验证码）
const res1 = await post('/login', {email, password});
if (res1.data.totpRequired) {
    // 弹出输入框让用户输入Google Authenticator动态码
    const code = prompt('请输入动态验证码');
    const res2 = await post('/login', {email, password, totpCode: code});
    // 保存 res2.data.accessToken
} else {
    // 直接保存 res1.data.accessToken
}
```

### 绑定 TOTP 流程

1. 登录后调用 `/totp/setup` 获取 `secret` 和 `qrCodeUrl`。
2. 使用 `qrCodeUrl` 生成二维码展示给用户（建议使用 qrcode.react 或 QRCode.js）。
3. 用户扫码后，输入 Authenticator 中显示的 6 位数字，调用 `/totp/confirm` 提交 `secret` 和 `code`。
4. 成功后，后续登录需提供动态码。

---

## 五、邮件模板（参考）

- **激活邮件**：包含链接 `https://your-frontend.com/verify-email?token=xxx`（前端路由负责解析并调用 `GET /verify-email`）。
- **重置密码邮件**：包含链接 `https://your-frontend.com/reset-password?token=xxx`（前端页面收集新密码后调用 `POST
  /reset-password`）。

> **提示**：后端仅生成 token，前端应负责构建完整 URL。

---

## 六、安全说明

- 密码使用 **BCrypt** 加密存储。
- JWT 有效期默认 **24 小时**，过期后需重新登录。
- 所有邮件 token 均为 **UUID**，有效期 **15 分钟**。
- 绑定 TOTP 后，登录必须提供动态验证码，有效防账户被盗。

---

**文档版本**：v1.0  
**更新日期**：2026-07-22
