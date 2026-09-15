package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.huizhipay.acquiring.entity.MerchantApiKey;
import com.huizhipay.acquiring.mapper.MerchantApiKeyMapper;
import com.huizhipay.common.exceptions.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantApiKeyServiceTest {
    @Test void issuesOneTimeTestSecretAndStoresOnlyItsDigest() throws Exception {
        MerchantApiKeyMapper mapper = mock(MerchantApiKeyMapper.class);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        MerchantApiKeyService.IssuedKey issued = new MerchantApiKeyService(mapper).issue("M-A");

        assertThat(issued.secretKey()).matches("^hzp_test_[A-Za-z0-9_-]{48}$");
        ArgumentCaptor<MerchantApiKey> saved = ArgumentCaptor.forClass(MerchantApiKey.class);
        verify(mapper).insert(saved.capture());
        assertThat(saved.getValue().getMerchantId()).isEqualTo("M-A");
        assertThat(saved.getValue().getKeyPrefix()).isEqualTo(issued.secretKey().substring(0, 20));
        assertThat(saved.getValue().getKeyHash()).isEqualTo(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(issued.secretKey().getBytes(StandardCharsets.UTF_8))));
        assertThat(saved.getValue().getKeyHash()).doesNotContain(issued.secretKey());
    }

    @Test void cannotDisableAnotherMerchantsOrAlreadyDisabledKey() {
        MerchantApiKeyMapper mapper = mock(MerchantApiKeyMapper.class);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(0);
        assertThatThrownBy(() -> new MerchantApiKeyService(mapper).disable("M-A", 99))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(404);
    }
}
