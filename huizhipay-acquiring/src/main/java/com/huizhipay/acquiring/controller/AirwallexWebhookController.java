package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.AcquiringWebhookService;
import com.huizhipay.common.crypto.AirwallexSignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AirwallexWebhookController {
    private final AcquiringWebhookService webhookService;

    // FIXME
    @Value("${airwallex.webhook.secret:1}")
    private String webhookSecret;

    public AirwallexWebhookController(AcquiringWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook/airwallex")
    public String handleWebhook(@RequestBody String rawBody,
                                @RequestHeader("x-signature") String signature) {
        // 1. 验签（防伪造）
        if (!AirwallexSignatureUtils.verifySignature(rawBody, signature, webhookSecret)) {
            log.warn("Airwallex 签名校验失败");
            return "FAIL"; // 返回非200，Airwallex会重试
        }

        // 2. 委托 Service 处理（包含幂等和记账）
        webhookService.processPaymentSuccess(rawBody);
        return "OK";
    }
}