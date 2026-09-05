package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 创建企业用户请求
 */
@Data
public class CreateBusinessUserRequest {
    /** 企业名称，必填 */
    private String businessName;
    /** 企业注册号，必填 */
    private String regNo;
    /** 成立日期，格式 DD-MM-YYYY */
    private String date;
    /** 企业邮箱，必填 */
    private String email;
    /** 联系电话，必填 */
    private String phone;
    /** 国际区号，必填 */
    private String phoneCode;
    /** 注册国家代码，必填 */
    private String country;
    /** 居住国家代码（可选，不传默认与 country 相同） */
    private String countryOfResidence;
    /** 注册地址，必填 */
    private Address address;
}
