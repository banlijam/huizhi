package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.dto.AcquiringRequest;
import com.huizhipay.acquiring.dto.AcquiringResponse;
import com.huizhipay.acquiring.factory.AcquiringFactory;
import com.huizhipay.common.event.PaymentAuthorizedEvent;
import com.huizhipay.common.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/acquiring")
@RequiredArgsConstructor
public class AcquiringController {
    private final AcquiringFactory acquiringFactory;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/checkout")
    public R<AcquiringResponse> checkout(@RequestBody AcquiringRequest request) {
        // 1. 获取对应网关的适配器执行支付
        AcquiringResponse response = acquiringFactory.getStrategy(request.getGatewayName()).executePay(request);
        // 2. 如果支付成功，解耦发布“付款授权成功”事件
        if ("AUTHORIZED".equals(response.getStatus())) {
            eventPublisher.publishEvent(new PaymentAuthorizedEvent(
                    "orderNo", request.getMerchantId(), request.getAmount(), request.getCurrency(), "request", System.currentTimeMillis()
            ));
        }
        return R.ok(response);
    }
}