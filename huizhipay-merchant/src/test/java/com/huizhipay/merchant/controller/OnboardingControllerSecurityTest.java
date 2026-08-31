package com.huizhipay.merchant.controller;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.merchant.dto.BindWalletRequest;
import com.huizhipay.merchant.service.MerchantService;
import com.huizhipay.settlement.service.SettlementWalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerSecurityTest {

    @Mock private MerchantResolver merchantResolver;
    @Mock private MerchantAccessGuard merchantAccessGuard;
    @Mock private MerchantService merchantService;
    @Mock private SettlementWalletService settlementWalletService;
    @InjectMocks private OnboardingController controller;

    @Test
    void readonlyCannotReplaceSettlementWallet() {
        doThrow(new BizException(403, "Forbidden"))
                .when(merchantAccessGuard).requireAnyRole("OWNER", "ADMIN");
        BindWalletRequest request = new BindWalletRequest();
        request.setType("METAMASK");
        request.setAddress("0x0000000000000000000000000000000000000001");

        assertThatThrownBy(() -> controller.bindWallet(request))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(403);
        verifyNoInteractions(settlementWalletService);
    }
}
