package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单费用结构
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderFees {
    /** 费率模式：percentage / fixed 等 */
    private String feeMode;
    /** 网络费 */
    private BigDecimal networkFee;
    /** 手续费 */
    private BigDecimal processingFee;
    /** 汇率（字符串） */
    private String rrFee;
    /** 固定费用明细 */
    private FixedFee fixedFee;

    /** 固定费用汇总 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FixedFee {
        /** 固定费用总额 */
        private Integer totalFixedFees;
        /** TransFi 收取的固定费用 */
        private Integer totalFixedTfFees;
        /** 渠道商收取的固定费用 */
        private Integer totalFixedCxFees;
        /** 固定费用币种 */
        private String fixedFeesCurrency;
        /** 固定费率明细 */
        private FixedFeeDetails fixedFeeDetails;
    }

    /** 固定费率明细 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FixedFeeDetails {
        /** 基础固定费率 */
        private Integer baseFeeFixedRate;
        /** TransFi 固定费率 */
        private Integer tfFeeFixedRate;
        /** 渠道商固定费率 */
        private Integer cxFeeFixedRate;
        /** 费率币种 */
        private String currency;
    }
}
