package com.huizhipay.acquiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huizhipay.ledger.service.LedgerTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WebhookSecurityTest {
    @Test void encryptedMerchantSecretRoundTripsAndRequiresDeploymentKey() {
        String key= Base64.getEncoder().encodeToString(new byte[32]);
        WebhookSecretCipher cipher=new WebhookSecretCipher(key);
        String encrypted=cipher.encrypt("whsec_once");
        assertThat(encrypted).doesNotContain("whsec_once");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("whsec_once");
        assertThatThrownBy(()->new WebhookSecretCipher("").encrypt("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test void endpointPolicyAllowsOnlyConfiguredHttpsHostAndRejectsLoopbackAtSendTime() {
        WebhookEndpointPolicy policy=new WebhookEndpointPolicy("merchant-test.example.test,localhost");
        assertThat(policy.validateConfiguration("https://merchant-test.example.test/webhooks/huizhipay").getHost()).isEqualTo("merchant-test.example.test");
        assertThatThrownBy(()->policy.validateConfiguration("http://merchant-test.example.test/hook")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->policy.validateConfiguration("https://evil.example/hook")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->policy.validateForSend("https://localhost/hook")).isInstanceOf(IllegalStateException.class);
    }

    @Test void checkoutSignatureUsesExactRawBytesAndHasNoDefaultSecret() throws Exception {
        TransFiCheckoutWebhookService service=new TransFiCheckoutWebhookService(mock(org.springframework.jdbc.core.JdbcTemplate.class),new ObjectMapper(),mock(LedgerTransferService.class),mock(MerchantWebhookService.class));
        ReflectionTestUtils.setField(service,"enabled",true);ReflectionTestUtils.setField(service,"webhookSecret","sandbox-secret");
        byte[] raw="{\"eventId\":\"evt_1\",\"amount\":\"12.00\"}".getBytes(StandardCharsets.UTF_8);
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec("sandbox-secret".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String signature=HexFormat.of().formatHex(mac.doFinal(raw));
        assertThat(service.signatureValid(raw,signature)).isTrue();
        assertThat(service.signatureValid(" {}".getBytes(StandardCharsets.UTF_8),signature)).isFalse();
        Object ambiguous=ReflectionTestUtils.invokeMethod(service,"terminal","payment.unsuccessful","unsuccessful");
        Object settled=ReflectionTestUtils.invokeMethod(service,"terminal","order","fund_settled");
        assertThat(ambiguous).isNull();
        assertThat(String.valueOf(settled)).isEqualTo("SUCCESS");
        ReflectionTestUtils.setField(service,"webhookSecret","");
        assertThat(service.enabled()).isFalse();
    }
}
