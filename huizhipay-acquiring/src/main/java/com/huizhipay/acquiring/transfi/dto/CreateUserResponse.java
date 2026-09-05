package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 创建用户响应数据（个人 / 企业用户通用）
 */
@Data
public class CreateUserResponse {
    /** 创建成功的用户 ID，如 UX-1123456789 */
    private String userId;
}
