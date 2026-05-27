package com.huizhipay.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体（对应表 user）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_user")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 登录邮箱（唯一）
     */
    private String email;
    /**
     * 加密后的密码（BCrypt）
     */
    private String password;
    /**
     * 用户昵称（可选）
     */
    private String nickname;
    /**
     * 邮箱是否已验证（0-未验证，1-已验证）
     */
    @TableField("email_verified")
    private Boolean emailVerified;
    /**
     * TOTP 密钥（Base32 编码，可为空）
     */
    @TableField("totp_secret")
    private String totpSecret;
    /**
     * 是否开启 Google Authenticator TOTP（0-关闭，1-开启）
     */
    @TableField("totp_enabled")
    private Boolean totpEnabled;
    /**
     * 账户状态：1-启用，0-禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}