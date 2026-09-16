package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.TestPaymentService;
import com.huizhipay.common.model.R;
import lombok.RequiredArgsConstructor;
import com.huizhipay.common.security.MerchantApiKeyAuthenticationPort.MerchantApiKeyPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/api/v1/test/payments")
@RequiredArgsConstructor
public class TestPaymentController {
    private final TestPaymentService paymentService;

    @PostMapping
    public R<TestPaymentService.PaymentView> create(
            @AuthenticationPrincipal MerchantApiKeyPrincipal principal,
            @RequestBody TestPaymentService.CreateCommand command) {
        return R.ok(paymentService.create(principal.merchantId(), command));
    }

    @GetMapping("/{merchantOrderNo}")
    public R<TestPaymentService.PaymentView> get(
            @AuthenticationPrincipal MerchantApiKeyPrincipal principal,
            @PathVariable String merchantOrderNo) {
        return R.ok(paymentService.get(principal.merchantId(), merchantOrderNo));
    }
}
