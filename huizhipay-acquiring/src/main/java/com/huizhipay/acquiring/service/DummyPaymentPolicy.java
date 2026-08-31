package com.huizhipay.acquiring.service;

import com.huizhipay.common.exceptions.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DummyPaymentPolicy {

    private final boolean checkoutResultEnabled;

    public DummyPaymentPolicy(
            @Value("${huizhipay.dummy.checkout-result-enabled:false}") boolean checkoutResultEnabled) {
        this.checkoutResultEnabled = checkoutResultEnabled;
    }

    public void requireBrowserResultSubmission() {
        if (!checkoutResultEnabled) {
            throw new BizException(403, "Browser-controlled Dummy payment results are disabled");
        }
    }
}
