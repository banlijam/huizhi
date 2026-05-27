package com.huizhipay.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "validate.token.not_blank")
    private String token;   // 邮件中的重置令牌
    @NotBlank(message = "validate.new_password.not_blank")
    @Size(min = 6, max = 20, message = "validate.password.length")
    private String newPassword;
}