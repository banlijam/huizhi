package com.huizhipay.ledger.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 单日统计结果。
 */
@Data
public class DailyStat {
    private LocalDate d;
    private Long successCount;
    private Long totalCount;
}
