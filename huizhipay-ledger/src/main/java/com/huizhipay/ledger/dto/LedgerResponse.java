package com.huizhipay.ledger.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 透明分账账本响应：全量累计的 KPI 总额 + 最近若干条明细行。
 */
@Data
@NoArgsConstructor
public class LedgerResponse {
    private BigDecimal totalGross;
    private BigDecimal totalFee;
    private BigDecimal totalNet;
    private long totalCount;
    private List<LedgerRowResponse> rows;
}
