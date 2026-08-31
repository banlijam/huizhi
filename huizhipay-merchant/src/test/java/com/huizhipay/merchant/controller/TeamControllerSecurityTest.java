package com.huizhipay.merchant.controller;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.common.security.MerchantResolver.MerchantAccess;
import com.huizhipay.merchant.dto.InviteMemberRequest;
import com.huizhipay.merchant.service.TeamService;
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
class TeamControllerSecurityTest {

    @Mock private MerchantResolver merchantResolver;
    @Mock private MerchantAccessGuard merchantAccessGuard;
    @Mock private TeamService teamService;
    @InjectMocks private TeamController controller;

    @Test
    void adminInvitesWithinServerResolvedMerchant() {
        InviteMemberRequest request = new InviteMemberRequest();
        request.setEmail("analyst@example.com");
        request.setRole("ANALYST");
        when(merchantAccessGuard.requireAnyRole("OWNER", "ADMIN"))
                .thenReturn(new MerchantAccess("M-A", "ADMIN"));

        controller.invite(request);

        verify(teamService).invite("M-A", request);
    }

    @Test
    void readonlyCannotInviteTeamMembers() {
        doThrow(new BizException(403, "Forbidden"))
                .when(merchantAccessGuard).requireAnyRole("OWNER", "ADMIN");

        assertThatThrownBy(() -> controller.invite(new InviteMemberRequest()))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(403);
        verifyNoInteractions(teamService);
    }
}
