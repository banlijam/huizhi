package com.huizhipay.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TotpConfirmRequest {
    @NotBlank(message = "validate.secret.not_blank")
    private String secret;
    @NotNull(message = "validate.code.not_null")
    private Integer code;   // 6位数字
}