package com.huizhipay.bootstrap.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SandboxStartupGuardTest {
    @Test
    void acceptsOnlyLocalSandboxDependencies() {
        MockEnvironment environment = validEnvironment();
        assertThatCode(() -> new SandboxStartupGuard(environment).run(null)).doesNotThrowAnyException();
    }

    @Test
    void rejectsProductionLikeDatabaseAndExternalMail() {
        MockEnvironment database = validEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:5432/huizhipay");
        assertThatThrownBy(() -> new SandboxStartupGuard(database).run(null))
                .hasMessageContaining("sandbox");

        MockEnvironment mail = validEnvironment().withProperty("spring.mail.host", "smtp.example.com");
        assertThatThrownBy(() -> new SandboxStartupGuard(mail).run(null))
                .hasMessageContaining("loopback SMTP");
    }

    @Test
    void checkoutOutboundNeedsExactEndpointAndConfirmedSandboxCredentials() {
        MockEnvironment unconfirmed = validEnvironment()
                .withProperty("huizhipay.transfi.checkout.outbound-enabled", "true")
                .withProperty("huizhipay.transfi.checkout.base-url", "https://checkout-server.transfi.com");
        assertThatThrownBy(() -> new SandboxStartupGuard(unconfirmed).run(null))
                .hasMessageContaining("credential confirmation");

        MockEnvironment valid = validEnvironment()
                .withProperty("huizhipay.transfi.checkout.outbound-enabled", "true")
                .withProperty("huizhipay.transfi.checkout.base-url", "https://checkout-server.transfi.com")
                .withProperty("huizhipay.transfi.checkout.sandbox-credentials-confirmed", "true")
                .withProperty("huizhipay.transfi.checkout.public-key", "pk_sandboxexample")
                .withProperty("huizhipay.transfi.checkout.secret-key", "sandbox_secret_at_least_24_chars")
                .withProperty("huizhipay.transfi.checkout.payment-link-id", "68f86e65a530b2baa9866831")
                .withProperty("huizhipay.sandbox.merchant-return-origin", "https://merchant-sandbox.example.test");
        assertThatCode(() -> new SandboxStartupGuard(valid).run(null)).doesNotThrowAnyException();
    }

    private MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("server.address", "127.0.0.1")
                .withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:15432/huizhipay_sandbox")
                .withProperty("spring.mail.host", "127.0.0.1")
                .withProperty("client.transfi.url", "https://sandbox-api.transfi.com/v3")
                .withProperty("huizhipay.transfi.outbound-enabled", "false");
    }
}
