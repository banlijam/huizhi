package com.huizhipay.acquiring.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AcquiringRequest {
    private String gatewayName;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
}