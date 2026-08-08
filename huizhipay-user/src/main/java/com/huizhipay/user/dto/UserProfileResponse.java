package com.huizhipay.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商户头部用户资料：可用余额 + 邮箱。
 */
@Data
@NoArgsConstructor
public class UserProfileResponse {
    private BigDecimal balance;
    private String email;
}
