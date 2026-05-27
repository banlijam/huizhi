package com.huizhipay.acquiring.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("t_payment_order")
public class PaymentOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String merchantId;
    private BigDecimal amount; // 单位：分
    private String currency;
    private String channel; // AIRWALLEX, WECHAT, ALIPAY
    private String fingerprint;
    private String channelTradeNo; // 存 Airwallex 的 payment_intent_id
    private PaymentStatusEnum status; // 0待支付 1成功 2失败 3关单
    private String clientSecret;
    private LocalDateTime expireAt;
    private String remark;
    @Version // 乐观锁注解，更新时会自动对 version + 1
    private Integer version;
    @TableLogic // 逻辑删除（查询时自动过滤 deleted=1 的数据）
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Getter
    @AllArgsConstructor
    public enum PaymentStatusEnum {
        PENDING("PENDING", "待支付"),
        SUCCESS("SUCCESS", "支付成功"),
        FAILED("FAILED", "支付失败"),
        CLOSED("CLOSED", "已关单");

        @EnumValue
        private final String code;
        @JsonValue
        private final String desc;
    }
}