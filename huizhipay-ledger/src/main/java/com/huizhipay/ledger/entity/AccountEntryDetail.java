package com.huizhipay.ledger.entity;

import com.huizhipay.ledger.entity.LedgerEntry.BizTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class AccountEntryDetail {
    private String accountNo;
    private String merchantId;
    private BigDecimal amount;
    private BizTypeEnum bizType;
    private String channel;
    private String externalOrderId;
    private String remark;
}
