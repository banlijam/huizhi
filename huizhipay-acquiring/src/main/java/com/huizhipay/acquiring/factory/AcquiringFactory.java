package com.huizhipay.acquiring.factory;

import com.huizhipay.acquiring.strategy.PaymentGatewayStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AcquiringFactory {
    private final Map<String, PaymentGatewayStrategy> strategyMap;

    public PaymentGatewayStrategy getStrategy(String gatewayName) {
        PaymentGatewayStrategy strategy = strategyMap.get(gatewayName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported acquiring gateway: " + gatewayName);
        }
        return strategy;
    }
}