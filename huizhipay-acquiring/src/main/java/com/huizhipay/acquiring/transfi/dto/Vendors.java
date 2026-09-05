package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 供应商账户信息（旧用户特有扩展）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vendors {
    /** 用户账户 */
    private String userAccount;
}
