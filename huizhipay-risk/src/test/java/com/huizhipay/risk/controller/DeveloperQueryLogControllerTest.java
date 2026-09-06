package com.huizhipay.risk.controller;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver.MerchantAccess;
import com.huizhipay.risk.dto.DeveloperQueryLogResponse;
import com.huizhipay.risk.service.DeveloperQueryLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeveloperQueryLogControllerTest {

    @Mock private MerchantAccessGuard merchantAccessGuard;
    @Mock private DeveloperQueryLogService developerQueryLogService;
    @InjectMocks private DeveloperQueryLogController controller;

    @Test
    void returnsOnlyLogsForTheResolvedMerchant() {
        when(merchantAccessGuard.requireAnyRole("OWNER", "ADMIN", "ANALYST", "READONLY"))
                .thenReturn(new MerchantAccess("M-A", "READONLY"));
        when(developerQueryLogService.latestForMerchant("M-A")).thenReturn(List.of());

        assertThat(controller.list().getData()).isEmpty();

        verify(developerQueryLogService).latestForMerchant("M-A");
    }

    @Test
    void rejectsUsersWithoutMerchantAccess() {
        doThrow(new BizException(403, "Forbidden"))
                .when(merchantAccessGuard).requireAnyRole("OWNER", "ADMIN", "ANALYST", "READONLY");

        assertThatThrownBy(() -> controller.list())
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(403);
        verifyNoInteractions(developerQueryLogService);
    }
}
