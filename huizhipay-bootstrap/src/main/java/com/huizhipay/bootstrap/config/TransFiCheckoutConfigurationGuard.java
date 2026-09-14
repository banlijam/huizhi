package com.huizhipay.bootstrap.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Validates only the TransFi Sandbox boundary when Checkout calls are enabled. */
@Component
public class TransFiCheckoutConfigurationGuard implements ApplicationRunner {
    private static final String CHECKOUT_URL = "https://checkout-server.transfi.com";
    private final Environment environment;

    public TransFiCheckoutConfigurationGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!environment.getProperty("huizhipay.transfi.checkout.outbound-enabled", Boolean.class, false)) {
            return;
        }
        if (!CHECKOUT_URL.equalsIgnoreCase(required("huizhipay.transfi.checkout.base-url"))) {
            throw new IllegalStateException("TransFi Checkout requires the fixed official HTTPS endpoint");
        }
        if (!environment.getProperty(
                "huizhipay.transfi.checkout.sandbox-credentials-confirmed", Boolean.class, false)) {
            throw new IllegalStateException("TransFi Checkout requires explicit Sandbox credential confirmation");
        }
        if (!required("huizhipay.transfi.checkout.public-key").matches("^pk_[A-Za-z0-9]+$")) {
            throw new IllegalStateException("TransFi Checkout public key format is invalid");
        }
        if (required("huizhipay.transfi.checkout.secret-key").length() < 24) {
            throw new IllegalStateException("TransFi Checkout secret key is missing or too short");
        }
        if (!required("huizhipay.transfi.checkout.payment-link-id").matches("^[a-f0-9]{24}$")) {
            throw new IllegalStateException("TransFi Checkout payment link id is invalid");
        }
        String returnOrigin = required("huizhipay.test.merchant-return-origin").toLowerCase(Locale.ROOT);
        if (!returnOrigin.matches("^https://[a-z0-9.-]+(?::[0-9]+)?$")) {
            throw new IllegalStateException("Test merchant return origin must be HTTPS without a path");
        }
    }

    private String required(String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("TransFi Checkout requires property: " + name);
        }
        return value.trim();
    }
}
