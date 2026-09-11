package com.huizhipay.acquiring.transfi.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.huizhipay.acquiring.transfi.dto.TransFiOrder;
import com.huizhipay.acquiring.transfi.dto.TransFiUser;
import lombok.Data;

/**
 * TransFi Webhook 通用事件载荷（User / Order 等 webhook 共用同一结构）。
 *
 * <p>文档：<a href="https://docs.transfi.com/docs/webhook-events-users">Users</a>、
 * <a href="https://docs.transfi.com/docs/webhook-events-orders">Orders</a></p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransFiWebhookEvent {

    /**
     * 事件唯一 ID，如 EV-231107XXXXX4387，可用于幂等
     */
    private String eventId;

    /**
     * 实体 ID（用户则是 UX-xxx，订单则是 OR-xxx）
     */
    private String entityId;

    /**
     * 实体类型：user / order
     */
    private String entityType;

    /**
     * 事件状态，取值示例：
     * <ul>
     *   <li>User: user_created / user_approved / user_rejected / kyc_initiated / kyc_pending /
     *       kyc_success / kyc_failed / kyc_manual_review / kyc_rejected / kyc_blocked / kyc_expired /
     *       kyb_initiated / kyb_pending / kyb_success / kyb_failed / kyb_manual_review / kyb_rejected / kyb_blocked / kyb_expired</li>
     *   <li>Order: initiated / fund_settled / fund_failed / cancelled ...</li>
     * </ul>
     */
    private String status;

    /**
     * 商户 MID
     */
    private String mid;

    /**
     * 用户对象（User webhook 时有效）
     */
    private TransFiUser user;

    /**
     * 订单对象（Order webhook 时有效，User webhook 中为空对象）
     */
    private TransFiOrder order;

    /**
     * 合作伙伴自定义上下文（原样透传）
     */
    private String partnerContext;
}
