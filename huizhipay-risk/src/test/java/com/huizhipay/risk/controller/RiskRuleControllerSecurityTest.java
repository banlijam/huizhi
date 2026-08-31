package com.huizhipay.risk.controller;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.common.security.MerchantResolver.MerchantAccess;
import com.huizhipay.risk.dto.ToggleRuleRequest;
import com.huizhipay.risk.service.RiskRuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskRuleControllerSecurityTest {

    @Mock private MerchantResolver merchantResolver;
    @Mock private MerchantAccessGuard merchantAccessGuard;
    @Mock private RiskRuleService riskRuleService;
    @InjectMocks private RiskRuleController controller;

    @Test
    void analystCanChangeRiskRuleOnlyForResolvedMerchant() {
        ToggleRuleRequest request = new ToggleRuleRequest();
        request.setEnabled(true);
        when(merchantAccessGuard.requireAnyRole("OWNER", "ADMIN", "ANALYST"))
                .thenReturn(new MerchantAccess("M-A", "ANALYST"));

        controller.toggle("strictMode", request);

        verify(riskRuleService).toggleRule("M-A", "strictMode", true);
    }

    @Test
    void readonlyCannotChangeRiskRules() {
        doThrow(new BizException(403, "Forbidden"))
                .when(merchantAccessGuard).requireAnyRole("OWNER", "ADMIN", "ANALYST");

        assertThatThrownBy(() -> controller.toggle("strictMode", new ToggleRuleRequest()))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(403);
        verifyNoInteractions(riskRuleService);
    }
}
