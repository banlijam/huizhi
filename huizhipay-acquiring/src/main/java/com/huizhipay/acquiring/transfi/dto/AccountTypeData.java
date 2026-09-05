package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 用户账户类型响应数据
 *
 * @see com.huizhipay.acquiring.transfi.TransFiClient#getAccountType(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTypeData {
    /** 用户 ID */
    private String id;
    /** 实体类型，如 USER */
    private String entityType;
    /** 账户角色：SENDER（资金发起方）/ RECIPIENT（资金接收方） */
    private String accountType;
}
