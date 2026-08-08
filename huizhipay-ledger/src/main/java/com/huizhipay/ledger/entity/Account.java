package com.huizhipay.ledger.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 账户表实体（对应表 t_account） */
@Data
@TableName("t_account")
public class Account {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountNo;
    private String merchantId;
    /** 账户类型 */
    private AccountTypeEnum accountType;
    private String currency;
    private BigDecimal balance;
    /** 乐观锁版本号（MyBatis-Plus 自动处理） */
    @Version
    private Integer version;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 账户类型：商户可用资产 / 托管负债 / 平台收入 / 平台成本 */
    public enum AccountTypeEnum {
        ASSET_AVAILABLE,
        LIABILITY_CUSTODY,
        PLATFORM_INCOME,
        PLATFORM_COST
    }
}
