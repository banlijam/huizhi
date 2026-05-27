package com.huizhipay.ledger.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ledger_entry")
public class LedgerEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantId;
    private String accountNo;
    /**
     * 正负金额：正数(+) = 入账/增加，负数(-) = 出账/扣减
     */
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String bizType;
    private String bizId;
    private String channel;
    private String externalOrderId;
    private String entryStatus;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}