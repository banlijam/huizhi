package com.huizhipay.ledger.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 指挥中心 - 今日大盘响应（对齐前端 controlRoom mock 结构）。
 */
@Data
@NoArgsConstructor
public class OverviewStatsResponse {
    private long todayCount;
    private double todayCountChange;
    private double conversionRate;
    private double conversionRateChange;
    private BigDecimal todayVolume;
    private double todayVolumeChange;
    private double settlementCountdownHours;
    private int settlementCountdownTotal;
    private SplitRatio splitRatio;
    private ChartData chartData;

    @Data
    @NoArgsConstructor
    public static class SplitRatio {
        private BigDecimal feeRate;
        private BigDecimal netRate;
        private String feeLabel;
        private String netLabel;
    }

    @Data
    @NoArgsConstructor
    public static class ChartData {
        private List<String> labels;
        private List<Long> requests;
        private List<Long> approved;
    }
}
