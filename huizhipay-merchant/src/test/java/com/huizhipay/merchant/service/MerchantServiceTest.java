package com.huizhipay.merchant.service;

import com.huizhipay.merchant.dto.SubmitOnboardingRequest;
import com.huizhipay.merchant.entity.Merchant;
import com.huizhipay.merchant.mapper.MerchantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {
    @Mock private MerchantMapper merchantMapper;

    @Test
    void resubmissionUsesTheMerchantOwnedByTheAuthenticatedUser() {
        Merchant owned = new Merchant().setId(9L).setMerchantId("M-OWNED").setOwnerUserId(7L)
                .setKybStatus(Merchant.KybStatus.PENDING);
        when(merchantMapper.selectOne(any())).thenReturn(owned);

        Merchant result = new MerchantService(merchantMapper).submit("M-UNTRUSTED", 7L, request());

        assertThat(result.getMerchantId()).isEqualTo("M-OWNED");
        assertThat(result.getKybStatus()).isEqualTo(Merchant.KybStatus.PENDING);
        verify(merchantMapper).updateById(owned);
        verify(merchantMapper, never()).insert(any(Merchant.class));
    }

    @Test
    void firstSubmissionCreatesOnePendingMerchantForTheOwner() {
        when(merchantMapper.selectOne(any())).thenReturn(null);
        new MerchantService(merchantMapper).submit(null, 7L, request());

        ArgumentCaptor<Merchant> inserted = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getOwnerUserId()).isEqualTo(7L);
        assertThat(inserted.getValue().getMerchantId()).startsWith("M-");
        assertThat(inserted.getValue().getKybStatus()).isEqualTo(Merchant.KybStatus.PENDING);
    }

    private SubmitOnboardingRequest request() {
        SubmitOnboardingRequest request = new SubmitOnboardingRequest();
        request.setCompany("Fresh Sandbox Merchant");
        request.setCountry("HK");
        request.setLicenseNo("TEST-001");
        request.setLegalRep("Test Owner");
        request.setIdNo("SANDBOX-ID");
        request.setSettlementPref(Merchant.SettlementPref.FIAT);
        return request;
    }
}
