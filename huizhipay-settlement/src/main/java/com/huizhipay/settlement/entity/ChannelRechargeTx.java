package com.huizhipay.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** 渠道充值交易（对应表 t_channel_recharge_tx） */
@Data
@TableName("t_channel_recharge_tx")
public class ChannelRechargeTx {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantId;
    private String channel;
    private String externalOrderId;
    private BigDecimal amount;
    private String currency;
    /** 渠道交易状态 */
    private ChannelTxStatus channelStatus;
    /** 存储原始回调 JSON，MyBatis-Plus Jackson 处理器自动序列化/反序列化 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawCallbackPayload;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 渠道交易状态：SUCCESS 成功 / FAILED 失败 / PENDING 处理中 */
    public enum ChannelTxStatus { SUCCESS, FAILED, PENDING }
}
