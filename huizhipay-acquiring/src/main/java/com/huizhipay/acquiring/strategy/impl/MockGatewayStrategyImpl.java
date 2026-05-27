package com.huizhipay.acquiring.strategy.impl;

import com.huizhipay.acquiring.dto.AcquiringRequest;
import com.huizhipay.acquiring.dto.AcquiringResponse;
import com.huizhipay.acquiring.strategy.PaymentGatewayStrategy;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MockGatewayStrategyImpl implements PaymentGatewayStrategy {
    @Override
    public String getGatewayName() {
        return "MOCK_CARD_GATEWAY";
    }

    @Override
    public AcquiringResponse executePay(AcquiringRequest request) {
        // 模拟 1.5 秒交易延迟
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        return new AcquiringResponse()
                .setStatus("AUTHORIZED")
                .setTransactionId("TXN_" + UUID.randomUUID()
                                               .toString()
                                               .replace("-", "")
                                               .substring(0, 10)
                                               .toUpperCase())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setMessage("Mock network authorized successfully.");
    }
}