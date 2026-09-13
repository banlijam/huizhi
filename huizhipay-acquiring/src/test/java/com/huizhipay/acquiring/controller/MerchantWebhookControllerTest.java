package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantWebhookService;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MerchantWebhookControllerTest {
    MerchantWebhookService service=mock(MerchantWebhookService.class);
    MerchantAccessGuard guard=mock(MerchantAccessGuard.class);
    MerchantResolver resolver=mock(MerchantResolver.class);
    MerchantWebhookController controller=new MerchantWebhookController(service,guard,resolver);

    @BeforeEach void setup(){ReflectionTestUtils.setField(controller,"frontendUrl","http://127.0.0.1:14328");when(guard.requireAnyRole("OWNER","ADMIN")).thenReturn(new MerchantResolver.MerchantAccess("M-A","OWNER"));}

    @Test void writeRejectsMissingOrCrossOriginCsrfProof(){
        MockHttpServletRequest request=new MockHttpServletRequest();request.addHeader("Origin","https://evil.example");request.addHeader("X-HuizhiPay-CSRF","1");
        assertThatThrownBy(()->controller.test(request)).isInstanceOf(BizException.class);
        verifyNoInteractions(service);
    }

    @Test void retryAlwaysUsesServerResolvedMerchant(){
        MockHttpServletRequest request=new MockHttpServletRequest();request.addHeader("Origin","http://127.0.0.1:14328");request.addHeader("X-HuizhiPay-CSRF","1");
        controller.retry("evt_other_merchant_guess",request);
        verify(service).retry("M-A","evt_other_merchant_guess");
    }
}
