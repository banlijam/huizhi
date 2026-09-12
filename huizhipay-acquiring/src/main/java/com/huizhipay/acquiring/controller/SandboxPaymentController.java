package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantApiKeyAuthenticator;
import com.huizhipay.acquiring.service.SandboxPaymentService;
import com.huizhipay.common.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("sandbox")
@RequestMapping("/api/v1/sandbox/payments")
@RequiredArgsConstructor
public class SandboxPaymentController {
    private final MerchantApiKeyAuthenticator apiKeyAuthenticator;
    private final SandboxPaymentService paymentService;

    @Value("${huizhipay.sandbox.merchant-return-origin:https://merchant-sandbox.example.test}")
    private String allowedReturnOrigin;

    @PostMapping
    public R<SandboxPaymentService.PaymentView> create(
            @RequestHeader(value = "X-HuizhiPay-Sandbox-Key", required = false) String apiKey,
            @RequestBody SandboxPaymentService.CreateCommand command) {
        String merchantId = apiKeyAuthenticator.requireMerchant(apiKey);
        return R.ok(paymentService.create(merchantId, command, allowedReturnOrigin));
    }

    @GetMapping("/{merchantOrderNo}")
    public R<SandboxPaymentService.PaymentView> get(
            @RequestHeader(value = "X-HuizhiPay-Sandbox-Key", required = false) String apiKey,
            @PathVariable String merchantOrderNo) {
        String merchantId = apiKeyAuthenticator.requireMerchant(apiKey);
        return R.ok(paymentService.get(merchantId, merchantOrderNo));
    }
}
