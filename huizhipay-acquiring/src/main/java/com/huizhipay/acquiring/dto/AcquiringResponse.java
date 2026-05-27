package com.huizhipay.acquiring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AcquiringResponse {
    private String status;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String message;
}