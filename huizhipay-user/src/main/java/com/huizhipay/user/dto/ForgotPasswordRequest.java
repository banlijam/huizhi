package com.huizhipay.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "validate.email.not_blank")
    @Email(message = "validate.email.format")
    private String email;
}