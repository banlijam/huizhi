package com.huizhipay.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 邮箱验证令牌实体（对应表 email_verification_token），用于注册激活和密码重置 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_email_verification_token")
public class EmailVerificationToken implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 关联用户ID */
    @TableField("user_id")
    private Long userId;
    /** 令牌（UUID） */
    private String token;
    /** 令牌类型：REGISTER 注册激活，RESET_PASSWORD 重置密码 */
    private TokenType type;
    /** 过期时间 */
    @TableField("expiry_date")
    private LocalDateTime expiryDate;
    /** 是否已使用 */
    private Boolean used;
    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public enum TokenType {
        REGISTER,
        RESET_PASSWORD
    }
}
