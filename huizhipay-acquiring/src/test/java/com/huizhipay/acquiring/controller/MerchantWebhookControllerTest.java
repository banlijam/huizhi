package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantWebhookService;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class MerchantWebhookControllerTest {
    MerchantWebhookService service=mock(MerchantWebhookService.class);
    MerchantAccessGuard guard=mock(MerchantAccessGuard.class);
    MerchantResolver resolver=mock(MerchantResolver.class);
    MerchantWebhookController controller=new MerchantWebhookController(service,guard,resolver);

    @BeforeEach void setup(){when(guard.requireAnyRole("OWNER","ADMIN")).thenReturn(new MerchantResolver.MerchantAccess("M-A","OWNER"));}

    @Test void retryAlwaysUsesServerResolvedMerchant(){
        controller.retry("evt_other_merchant_guess");
        verify(service).retry("M-A","evt_other_merchant_guess");
    }
}
