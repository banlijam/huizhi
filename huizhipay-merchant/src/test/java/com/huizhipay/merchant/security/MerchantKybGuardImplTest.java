package com.huizhipay.merchant.security;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.merchant.entity.Merchant;
import com.huizhipay.merchant.mapper.MerchantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantKybGuardImplTest {
    @Mock private MerchantMapper merchantMapper;

    @ParameterizedTest
    @EnumSource(value = Merchant.KybStatus.class, names = {"DRAFT", "PENDING", "REJECTED"})
    void incompleteKybStatusesCannotInitiateTransactions(Merchant.KybStatus status) {
        when(merchantMapper.selectOne(any())).thenReturn(
                new Merchant().setMerchantId("M-TEST").setKybStatus(status));
        MerchantKybGuardImpl guard = new MerchantKybGuardImpl(merchantMapper);

        assertThatThrownBy(() -> guard.requireApproved("M-TEST"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("KYB")
                .extracting("code").isEqualTo(403);

        assertMerchantIdLookup("M-TEST");
    }

    @Test
    void approvedMerchantCanInitiateTransactions() {
        when(merchantMapper.selectOne(any())).thenReturn(
                new Merchant().setMerchantId("M-APPROVED").setKybStatus(Merchant.KybStatus.APPROVED));
        MerchantKybGuardImpl guard = new MerchantKybGuardImpl(merchantMapper);

        assertThatCode(() -> guard.requireApproved("M-APPROVED")).doesNotThrowAnyException();

        assertMerchantIdLookup("M-APPROVED");
    }

    @Test
    void missingMerchantFailsClosed() {
        when(merchantMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> new MerchantKybGuardImpl(merchantMapper).requireApproved("M-MISSING"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(403);
    }

    private void assertMerchantIdLookup(String merchantId) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Merchant>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(merchantMapper).selectOne(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("merchant_id");
        AbstractWrapper<?, ?, ?> abstractQuery = (AbstractWrapper<?, ?, ?>) query.getValue();
        assertThat(abstractQuery.getParamNameValuePairs()).containsValue(merchantId);
    }
}
