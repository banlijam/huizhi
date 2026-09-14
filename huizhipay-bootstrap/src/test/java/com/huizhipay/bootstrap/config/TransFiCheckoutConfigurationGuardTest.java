package com.huizhipay.bootstrap.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransFiCheckoutConfigurationGuardTest {
    @Test
    void disabledCheckoutDoesNotImposeAnotherApplicationEnvironment() {
        assertThatCode(() -> new TransFiCheckoutConfigurationGuard(new MockEnvironment()).run(null))
                .doesNotThrowAnyException();
    }

    @Test
    void enabledCheckoutRequiresConfirmedSandboxCredentialsAndFixedEndpoint() {
        MockEnvironment unconfirmed = enabledEnvironment()
                .withProperty("huizhipay.transfi.checkout.sandbox-credentials-confirmed", "false");
        assertThatThrownBy(() -> new TransFiCheckoutConfigurationGuard(unconfirmed).run(null))
                .hasMessageContaining("Sandbox credential confirmation");

        MockEnvironment wrongEndpoint = enabledEnvironment()
                .withProperty("huizhipay.transfi.checkout.base-url", "https://api.transfi.com");
        assertThatThrownBy(() -> new TransFiCheckoutConfigurationGuard(wrongEndpoint).run(null))
                .hasMessageContaining("fixed official HTTPS endpoint");
    }

    @Test
    void acceptsConfirmedTransFiSandboxConfigurationInTheExistingTestEnvironment() {
        assertThatCode(() -> new TransFiCheckoutConfigurationGuard(enabledEnvironment()).run(null))
                .doesNotThrowAnyException();
    }

    private MockEnvironment enabledEnvironment() {
        return new MockEnvironment()
                .withProperty("huizhipay.transfi.checkout.outbound-enabled", "true")
                .withProperty("huizhipay.transfi.checkout.base-url", "https://checkout-server.transfi.com")
                .withProperty("huizhipay.transfi.checkout.sandbox-credentials-confirmed", "true")
                .withProperty("huizhipay.transfi.checkout.public-key", "pk_sandboxexample")
                .withProperty("huizhipay.transfi.checkout.secret-key", "sandbox_secret_at_least_24_chars")
                .withProperty("huizhipay.transfi.checkout.payment-link-id", "68f86e65a530b2baa9866831")
                .withProperty("huizhipay.test.merchant-return-origin", "https://merchant-test.example.test");
    }
}
