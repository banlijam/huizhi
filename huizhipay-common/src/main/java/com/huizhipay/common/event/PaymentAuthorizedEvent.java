package com.huizhipay.common.event;

import java.math.BigDecimal;

/**
 * @param channelTradeNo Airwallex 流水号
 */
public record PaymentAuthorizedEvent(String orderNo, String merchantId, BigDecimal amount, String currency,
                                     String channelTradeNo, Long timestamp) {
}
