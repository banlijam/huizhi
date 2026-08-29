package com.huizhipay.acquiring.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 收单支付订单（对应表 t_payment_order） */
@Data
@Accessors(chain = true)
@TableName("t_payment_order")
public class PaymentOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    /** 买家 Checkout 使用的公开随机令牌，不暴露内部订单号 */
    private String checkoutToken;
    /** Checkout 完成后返回商户网站的目标地址 */
    private String returnUrl;
    private String merchantId;
    /** 主币单位（元/美元） */
    private BigDecimal amount;
    private String currency;
    /** 渠道：AIRWALLEX / WECHAT / ALIPAY 等 */
    private String channel;
    private String fingerprint;
    /** 存 Airwallex 的 payment_intent_id */
    private String channelTradeNo;
    /** 支付状态 */
    private PaymentStatus status;
    private String clientSecret;
    private LocalDateTime expireAt;
    private String remark;
    /** 乐观锁（更新时自动 version+1） */
    @Version
    private Integer version;
    /** 逻辑删除（查询时自动过滤 deleted=1 的数据） */
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 支付状态：待支付 / 成功 / 失败 / 关单 */
    public enum PaymentStatus { PENDING, SUCCESS, FAILED, CLOSED }
}
