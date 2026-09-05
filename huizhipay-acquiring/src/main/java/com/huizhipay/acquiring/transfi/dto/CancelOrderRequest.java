package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 取消订单请求（仅 offramp 类型、且尚未收到加密货币存款的订单可取消）
 */
@Data
public class CancelOrderRequest {
    /** 订单 ID，必填 */
    private String orderId;
    /** 取消原因（自由文本，用于审计） */
    private String reason;
    /** 执行取消操作的用户或系统标识 */
    private String cancelledBy;
}
