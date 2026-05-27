package com.huizhipay.acquiring.strategy;

import com.huizhipay.acquiring.dto.AcquiringRequest;
import com.huizhipay.acquiring.dto.AcquiringResponse;

/**
 * 策略接口：未来接入 CCBill, Pacypay, Bankera 均需实现此接口
 */
public interface PaymentGatewayStrategy {
    String getGatewayName();

    AcquiringResponse executePay(AcquiringRequest request);
}