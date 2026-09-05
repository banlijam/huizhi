package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 取消订单响应数据
 */
@Data
public class CancelOrderResponse {
    /** 被取消的订单 ID */
    private String orderId;
    /** 取消后的订单状态：cancelled */
    private String status;
    /** 取消结果描述，如 Order cancelled successfully */
    private String message;
}
