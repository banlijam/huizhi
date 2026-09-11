package com.huizhipay.acquiring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huizhipay.acquiring.transfi.webhook.TransFiWebhookEvent;
import com.huizhipay.common.crypto.AirwallexSignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

/**
 * TransFi Webhook 回调入口。
 *
 * <p>签名方式：HMAC-SHA256(secret, rawBody) → hex，请求头 {@code X-Transfi-Hmac-Hash}。
 * 验签算法与 Airwallex 完全一致，直接复用 {@link AirwallexSignatureUtils}。</p>
 *
 * <p>文档：
 * <a href="https://docs.transfi.com/docs/configuring-a-webhook-listener">Configuring Webhook Listener</a>、
 * <a href="https://docs.transfi.com/docs/webhook-signature">Webhook Signature</a>、
 * <a href="https://docs.transfi.com/docs/webhook-events-users">User Events</a>
 * </p>
 */
@Slf4j
@RestController
public class TransFiWebhookController {

    /** 请求头：TransFi webhook 签名 */
    public static final String HEADER_TRANSFI_HMAC = "X-Transfi-Hmac-Hash";

    private final ObjectMapper objectMapper;

    @Value("${transfi.webhook.secret:1}")
    private String webhookSecret;

    /**
     * 进程内简单幂等集合（仅开发/单实例场景够用；生产应落库唯一 eventId）。
     */
    private final Set<String> PROCESSED_EVENT_IDS = new HashSet<>();

    public TransFiWebhookController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * TransFi 所有 webhook 统一入口，通过 {@code entityType} + {@code status} 路由分发。
     */
    @PostMapping("/webhook/transfi")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = HEADER_TRANSFI_HMAC, required = false) String signature) {

        // 1. 验签
        if (signature == null || !AirwallexSignatureUtils.verifySignature(rawBody, signature, webhookSecret)) {
            log.warn("TransFi webhook 签名校验失败，header={}", signature);
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // 2. 反序列化
        TransFiWebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, TransFiWebhookEvent.class);
        } catch (Exception e) {
            log.error("TransFi webhook body 解析失败: {}", rawBody, e);
            return ResponseEntity.status(400).body("Invalid body");
        }

        String eventId = event.getEventId();
        log.info("收到 TransFi webhook: eventId={}, entityType={}, status={}, entityId={}",
                eventId, event.getEntityType(), event.getStatus(), event.getEntityId());

        // 3. 幂等（进程内，生产需落库）
        if (eventId != null && !PROCESSED_EVENT_IDS.add(eventId)) {
            log.info("TransFi webhook 重复事件忽略: eventId={}", eventId);
            return ResponseEntity.ok("OK");
        }

        // 4. 分发处理
        try {
            dispatch(event);
        } catch (Exception e) {
            log.error("TransFi webhook 处理异常: eventId={}", eventId, e);
            return ResponseEntity.status(500).body("Processing failed");
        }

        return ResponseEntity.ok("OK");
    }

    /**
     * 按 entityType + status 分发到各业务处理方法。
     * 当前仅做日志占位，后续接入业务 Service 时在此处扩展。
     */
    private void dispatch(TransFiWebhookEvent event) {
        String entityType = event.getEntityType();
        String status = event.getStatus();
        if (entityType == null) {
            log.warn("未知 entityType: {}", event);
            return;
        }
        switch (entityType) {
            case "user":
                handleUserEvent(event, status);
                break;
            case "order":
                handleOrderEvent(event, status);
                break;
            default:
                log.warn("未处理的 entityType={}, status={}", entityType, status);
        }
    }

    private void handleUserEvent(TransFiWebhookEvent event, String status) {
        // TODO: 委托 UserWebhookService 处理，如 user_created/user_approved/kyc_success 等
        log.info("[UserWebhook] userId={}, status={}",
                event.getUser() != null ? event.getUser().getUserId() : null, status);
    }

    private void handleOrderEvent(TransFiWebhookEvent event, String status) {
        // TODO: 委托 OrderWebhookService 处理，如 fund_settled/fund_failed/cancelled 等
        log.info("[OrderWebhook] orderId={}, status={}",
                event.getOrder() != null ? event.getOrder().getId() : null, status);
    }
}
