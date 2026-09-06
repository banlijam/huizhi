package com.huizhipay.risk.dto;

import com.huizhipay.risk.entity.QueryLog.QueryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Safe, merchant-facing view of one billed API query. */
public record DeveloperQueryLogResponse(
        String queryNo,
        String productId,
        BigDecimal costAmount,
        QueryStatus status,
        String errorMessage,
        LocalDateTime createdAt
) {
}
