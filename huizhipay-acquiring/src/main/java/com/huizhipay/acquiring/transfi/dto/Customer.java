package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 用户 KYC/KYB 等校验后的客户信息（旧用户特有扩展）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {
    /** 已注册邮箱列表 */
    private java.util.List<String> email;
}
