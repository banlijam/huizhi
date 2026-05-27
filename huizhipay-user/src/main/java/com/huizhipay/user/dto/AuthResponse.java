package com.huizhipay.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AuthResponse {
    private String accessToken;    // JWT，未完成二次验证时为null
    private boolean totpRequired;  // 是否要求提供TOTP验证码
}