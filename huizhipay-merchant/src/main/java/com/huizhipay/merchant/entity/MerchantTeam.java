package com.huizhipay.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 商户团队成员 */
@Data
@Accessors(chain = true)
@TableName("t_merchant_team")
public class MerchantTeam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantId;
    private String email;
    /** 团队角色 */
    private TeamRole role;
    /** 邀请状态 */
    private TeamInviteStatus status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime sentOn;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 团队角色：管理员 / 分析员 / 只读 */
    public enum TeamRole { ADMIN, ANALYST, READONLY }

    /** 邀请状态：已接受 / 待接受 */
    public enum TeamInviteStatus { ACCEPTED, PENDING }
}
