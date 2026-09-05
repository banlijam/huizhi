package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 创建个人用户请求
 */
@Data
public class CreateIndividualUserRequest {
    /** 名 */
    private String firstName;
    /** 姓 */
    private String lastName;
    /** 出生日期，格式 DD-MM-YYYY */
    private String date;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 国际区号，如 +1 */
    private String phoneCode;
    /** 用户国籍代码，如 US（必填） */
    private String country;
    /** 居住国家代码（可选，不传默认与 country 相同） */
    private String countryOfResidence;
    /** 性别：male / female（可选） */
    private String gender;
    /** 地址 */
    private Address address;
}
