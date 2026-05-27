package com.huizhipay.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpSetupResponse {
    private String secret;      // Base32密钥，需保存至确认绑定
    private String qrCodeUrl;   // otpauth://格式，用于生成二维码
}