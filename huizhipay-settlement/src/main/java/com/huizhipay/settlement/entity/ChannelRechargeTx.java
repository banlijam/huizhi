package com.huizhipay.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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
    private String channelStatus;
    /**
     * 存储原始回调 JSON，利用 MyBatis-Plus 的 Jackson 处理器自动序列化/反序列化
     * 该注解为必要注解，用于声明 JSONB 类型映射
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawCallbackPayload;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}