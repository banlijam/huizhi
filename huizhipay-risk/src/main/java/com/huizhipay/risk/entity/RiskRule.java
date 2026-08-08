package com.huizhipay.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 商户风控规则开关 */
@Data
@Accessors(chain = true)
@TableName("t_risk_rule")
public class RiskRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantId;
    /** 风控规则ID */
    private RiskRuleId ruleId;
    private Boolean enabled;
    /** 规则分类 */
    private RiskCategory category;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 风控规则ID：严格模式 / 预付费卡拦截 / 强制3DS / KYT筛查 / 高风险地区拦截 */
    public enum RiskRuleId { STRICT_MODE, BLOCK_PREPAID, FORCE_US_3DS, KYT_SCREENING, BLOCK_HIGH_RISK_REGION }

    /** 规则分类：MASTER 主开关 / NORMAL 常规 */
    public enum RiskCategory { MASTER, NORMAL }
}
