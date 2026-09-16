package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantApiKeyService;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantKybGuard;
import com.huizhipay.common.security.MerchantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantApiKeyControllerTest {
    MerchantApiKeyService service = mock(MerchantApiKeyService.class);
    MerchantAccessGuard guard = mock(MerchantAccessGuard.class);
    MerchantKybGuard kyb = mock(MerchantKybGuard.class);
    Authentication auth = mock(Authentication.class);
    MerchantApiKeyController controller = new MerchantApiKeyController(service, guard, kyb);

    @BeforeEach void setup() {
        when(auth.getName()).thenReturn("owner@example.test");
        when(guard.requireAnyRole("OWNER", "ADMIN"))
                .thenReturn(new MerchantResolver.MerchantAccess("M-A", "OWNER"));
    }

    @Test void issueUsesAuthenticatedMerchantAndRequiresApprovedKyb() {
        var command = new MerchantApiKeyService.IssueCommand("TEST", "External", null, null);
        controller.issue(auth, command);
        verify(kyb).requireApproved("M-A");
        verify(service).issuePending("M-A", "owner@example.test", command);
    }

    @Test void disableNeverAcceptsMerchantIdFromTheRequest() {
        var command = new MerchantApiKeyService.RevokeCommand("compromised");
        controller.revoke(42, auth, command);
        verify(service).revoke("M-A", 42, "owner@example.test", "compromised");
    }
}
