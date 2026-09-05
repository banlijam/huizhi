package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 更新个人用户请求（用于修正 firstName、lastName、dob；更新 dob 会自动触发合规复审）
 */
@Data
public class UpdateIndividualUserRequest {
    /** 用户唯一标识，必填，如 UX-251218034104810 */
    private String userId;
    /** 修正后的名（可选） */
    private String firstName;
    /** 修正后的姓（可选） */
    private String lastName;
    /** 修正后的出生日期，格式 YYYY-MM-DD（可选；必须为有效日期） */
    private String dob;
}
