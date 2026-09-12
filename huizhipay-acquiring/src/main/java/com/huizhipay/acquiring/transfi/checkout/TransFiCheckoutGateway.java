package com.huizhipay.acquiring.transfi.checkout;

import java.math.BigDecimal;

public interface TransFiCheckoutGateway {
    Result createInvoice(Command command);

    record Command(String platformOrderNo, String merchantOrderNo, BigDecimal amount,
                   String currency, String successRedirectUrl, String failureRedirectUrl,
                   String productName) {}

    record Result(String channelOrderId, String paymentUrl) {}
}
