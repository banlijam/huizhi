package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 用户地址
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {
    /** 街道 */
    private String street;
    /** 城市 */
    private String city;
    /** 州/省 */
    private String state;
    /** 邮政编码 */
    private String postalCode;
    /** 邮政编码（部分响应使用 postCode 字段名） */
    private String postCode;
    /** 居住国家代码 */
    private String residenceCountry;
    /** 国家代码（部分接口返回） */
    private String country;
    /** 格式化后的完整地址 */
    private String formattedAddress;
}
