package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * 订单源（付款方）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderSource {
    /** 源货币代码（法币或加密货币） */
    private String currency;
    /** 源金额（字符串，避免精度丢失） */
    private String amount;
    /** 源用户 ID，第三方 offramp 流程使用 */
    private String userId;
    /** 支付方式代码（从 /v3/payment-methods 获取，法币场景使用） */
    private String paymentCode;
    /** 支付类型：bank_transfer / card / local_wallet（法币场景使用） */
    private String paymentType;
    /** 额外支付详情，headless 模式和 fiat_prefund 时必填 */
    private Map<String, Object> additionalPaymentDetails;
    /** 加密货币发送者钱包地址 */
    private String sendersWalletAddress;
    /** 钱包地址（sendersWalletAddress 的别名） */
    private String walletAddress;
}
