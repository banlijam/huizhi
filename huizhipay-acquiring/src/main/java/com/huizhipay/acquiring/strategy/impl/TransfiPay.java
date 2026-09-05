package com.huizhipay.acquiring.strategy.impl;

import com.huizhipay.acquiring.dto.AcquiringRequest;
import com.huizhipay.acquiring.dto.AcquiringResponse;
import com.huizhipay.acquiring.strategy.PaymentGatewayStrategy;
import com.huizhipay.acquiring.transfi.TransFiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransfiPay implements PaymentGatewayStrategy {
    private final TransFiClient transFiClient;

    @Override
    public String getGatewayName() {
        return "";
    }

    @Override
    public AcquiringResponse executePay(AcquiringRequest request) {
        return null;
    }
}
