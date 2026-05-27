package com.huizhipay.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "validate.email.not_blank")
    @Email(message = "validate.email.format")
    private String email;

    @NotBlank(message = "validate.password.not_blank")
    @Size(min = 6, max = 20, message = "validate.password.length")
    private String password;

    @Size(max = 64, message = "validate.nickname.length")
    private String nickname; // 可选
}