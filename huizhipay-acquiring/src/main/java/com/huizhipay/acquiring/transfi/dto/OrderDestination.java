package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * 订单目标（收款方）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDestination {
    /** 目标货币代码（法币或加密货币） */
    private String currency;
    /** 目标金额（字符串，避免精度丢失） */
    private String amount;
    /** 结算持有货币，仅 payin 和 gaming 适用；不传则与 currency 相同 */
    private String holdingCurrency;
    /** 目标用户 ID，第三方 offramp 流程使用 */
    private String userId;
    /** 支付方式代码（从 /v3/payment-methods 获取，法币场景使用） */
    private String paymentCode;
    /** 支付类型：bank_transfer / card / local_wallet（法币场景使用） */
    private String paymentType;
    /** 额外支付详情；加密场景下包含 walletOwner(exchange/self)、exchangeName、userConfirmed 等 */
    private Map<String, Object> additionalPaymentDetails;
    /** 加密货币目标钱包地址 */
    private String walletAddress;
    /** QR Payout 场景下的 QR 码地址 */
    private String qrCode;
}
