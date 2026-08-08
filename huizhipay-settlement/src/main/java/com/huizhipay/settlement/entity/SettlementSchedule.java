package com.huizhipay.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** T+1 清算计划（供清算倒计时与分账金额展示） */
@Data
@Accessors(chain = true)
@TableName("t_settlement_schedule")
public class SettlementSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantId;
    private LocalDate settlementDate;
    /** 预计到账时间（UTC） */
    private LocalDateTime expectedAt;
    private BigDecimal grossAmount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    /** 清算状态 */
    private SettlementStatusEnum status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 清算状态：PENDING 待清算 / SETTLED 已清算 */
    public enum SettlementStatusEnum {
        PENDING,
        SETTLED
    }
}
