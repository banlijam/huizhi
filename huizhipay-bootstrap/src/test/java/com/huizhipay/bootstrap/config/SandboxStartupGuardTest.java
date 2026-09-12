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

    private MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("server.address", "127.0.0.1")
                .withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:15432/huizhipay_sandbox")
                .withProperty("spring.mail.host", "127.0.0.1")
                .withProperty("client.transfi.url", "https://sandbox-api.transfi.com/v3")
                .withProperty("huizhipay.transfi.outbound-enabled", "false");
    }
}
