package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 用户元数据（旧用户特有扩展）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaData {
    /** 用户本地货币代码 */
    private String localCurrency;
    /** 是否为营收用户 */
    private Boolean isRevenueUser;
    /** 注册 IP */
    private String ip;
}
