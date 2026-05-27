package com.huizhipay.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "validate.email.not_blank")
    @Email(message = "validate.email.format")
    private String email;
    @NotBlank(message = "validate.password.not_blank")
    private String password;
    private Integer totpCode; // 可选，仅当用户开启TOTP时必填
}