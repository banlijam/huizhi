package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 更新个人用户响应数据
 */
@Data
public class UpdateUserResponse {
    /** 确认后的名 */
    private String firstName;
    /** 确认后的姓 */
    private String lastName;
    /** 确认后的出生日期，格式 YYYY-MM-DD */
    private String dob;
}
