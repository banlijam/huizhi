package com.huizhipay.ledger.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 时间区间统计结果（MyBatis 自动映射下划线 → 驼峰）。
 */
@Data
public class OverviewRangeStat {
    private Long successCount;
    private Long totalCount;
    private BigDecimal successVolume;
}
