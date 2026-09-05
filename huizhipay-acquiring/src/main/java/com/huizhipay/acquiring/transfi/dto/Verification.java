package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 邮箱或手机号验证状态
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Verification {
    /** 验证状态：verified（已验证）、initiated（已发起验证）、null（未发起） */
    private String status;
    /** 验证完成时间（ISO 8601），未验证时为 null */
    private String at;
}
