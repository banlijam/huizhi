package com.huizhipay.acquiring.service;

import com.huizhipay.acquiring.entity.MerchantApiKey;
import com.huizhipay.acquiring.mapper.MerchantApiKeyMapper;
import com.huizhipay.common.exceptions.BizException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantApiKeyAuthenticatorTest {
    @Test void resolvesMerchantFromStoredDigestWithoutPersistingRawKey() throws Exception {
        MerchantApiKeyMapper mapper = mock(MerchantApiKeyMapper.class);
        String raw = "hzp_test_" + "a".repeat(48);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
        when(mapper.selectOne(any())).thenReturn(new MerchantApiKey().setId(1L).setMerchantId("M-A").setKeyHash(hash).setEnabled(true).setStatus("ACTIVE").setScopes("payments:read payments:write"));
        assertThat(new MerchantApiKeyAuthenticator(mapper).authenticate(raw,"TEST","payments:write").merchantId()).isEqualTo("M-A");
    }

    @Test void malformedKeyFailsBeforeDatabaseLookup() {
        MerchantApiKeyMapper mapper = mock(MerchantApiKeyMapper.class);
        assertThatThrownBy(() -> new MerchantApiKeyAuthenticator(mapper).authenticate("short","TEST","payments:write"))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(401);
        verifyNoInteractions(mapper);
    }
}
