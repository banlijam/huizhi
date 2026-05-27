package com.huizhipay.ledger.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huizhipay.common.enums.AccountTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_account")
public class Account {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountNo;
    private String merchantId;
    private AccountTypeEnum accountType;
    private String currency;
    private BigDecimal balance;
    /**
     * 乐观锁版本号（MyBatis-Plus 自动处理）
     */
    @Version
    private Integer version;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}