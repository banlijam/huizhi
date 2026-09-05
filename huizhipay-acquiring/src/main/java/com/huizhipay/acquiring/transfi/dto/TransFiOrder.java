package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * TransFi 订单（onramp / offramp / payin / payout / prefund / swap / gaming）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransFiOrder {
    /** 订单唯一标识，如 OR-2603071056090301428 */
    private String id;
    /** 订单整体状态：initiated / fund_settled / fund_failed / cancelled 等 */
    private String status;
    /** 订单类型（API 响应字段名为 type）：onramp / offramp / payin / payout / fiat_prefund / crypto_prefund / swap / gaming */
    private String type;
    /** 订单类型（创建请求字段名为 orderType，与 type 语义相同，留作双向映射） */
    private String orderType;
    /** 支付目的代码 */
    private String purposeCode;
    /** 目的代码的可读描述 */
    private String purposeCodeReason;
    /** 源（付款方） */
    private OrderSource source;
    /** 目标（收款方） */
    private OrderDestination destination;
    /** 关联的用户 ID */
    private String userId;
    /** 发送者姓名 */
    private SenderName senderName;
    /** 商户 MID */
    private String mid;
    /** 费用信息 */
    private OrderFees fees;
    /** 失败代码 */
    private String failureCode;
    /** 失败原因描述 */
    private String failureMessage;
    /** 合作伙伴自定义上下文数据（原样透传） */
    private Map<String, Object> partnerContext;
    /** 订单创建时间 */
    private Instant createdAt;
    /** 订单最后更新时间 */
    private Instant updatedAt;
}
