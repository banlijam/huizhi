package com.huizhipay.acquiring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("t_payment_event_log")
public class PaymentEventLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String merchantId;
    private String eventType;
    private String transactionId;
    private LocalDateTime createdAt;
}
