package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantApiKeyService;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantKybGuard;
import com.huizhipay.common.security.MerchantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantApiKeyControllerTest {
    MerchantApiKeyService service = mock(MerchantApiKeyService.class);
    MerchantAccessGuard guard = mock(MerchantAccessGuard.class);
    MerchantKybGuard kyb = mock(MerchantKybGuard.class);
    MerchantApiKeyController controller = new MerchantApiKeyController(service, guard, kyb);

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://127.0.0.1:3000");
        when(guard.requireAnyRole("OWNER", "ADMIN"))
                .thenReturn(new MerchantResolver.MerchantAccess("M-A", "OWNER"));
    }

    @Test void issueRejectsMissingOrCrossOriginCsrfProof() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.example");
        request.addHeader("X-HuizhiPay-CSRF", "1");
        assertThatThrownBy(() -> controller.issue(request)).isInstanceOf(BizException.class);
        verifyNoInteractions(service, kyb);
    }

    @Test void issueUsesAuthenticatedMerchantAndRequiresApprovedKyb() {
        MockHttpServletRequest request = validRequest();
        controller.issue(request);
        verify(kyb).requireApproved("M-A");
        verify(service).issue("M-A");
    }

    @Test void disableNeverAcceptsMerchantIdFromTheRequest() {
        controller.disable(42, validRequest());
        verify(service).disable("M-A", 42);
    }

    private MockHttpServletRequest validRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "http://127.0.0.1:3000");
        request.addHeader("X-HuizhiPay-CSRF", "1");
        return request;
    }
}
