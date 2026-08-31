package com.huizhipay.common.security;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantResolver.MerchantAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantAccessGuardTest {

    @Mock private MerchantResolver merchantResolver;
    private MerchantAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new MerchantAccessGuard(merchantResolver);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN"})
    void ownerAndAdminCanPerformAdministrativeWrites(String role) {
        MerchantAccess access = new MerchantAccess("M-A", role);
        when(merchantResolver.getCurrentMerchantAccess()).thenReturn(access);

        assertThat(guard.requireAnyRole("OWNER", "ADMIN")).isSameAs(access);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN", "ANALYST"})
    void analystIsLimitedToExplicitlyAllowedOperationalWrites(String role) {
        MerchantAccess access = new MerchantAccess("M-A", role);
        when(merchantResolver.getCurrentMerchantAccess()).thenReturn(access);

        assertThat(guard.requireAnyRole("OWNER", "ADMIN", "ANALYST")).isSameAs(access);
    }

    @Test
    void readonlyRoleFailsClosedForWrites() {
        when(merchantResolver.getCurrentMerchantAccess())
                .thenReturn(new MerchantAccess("M-A", "READONLY"));

        assertThatThrownBy(() -> guard.requireAnyRole("OWNER", "ADMIN", "ANALYST"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(403);
    }

    @Test
    void missingMerchantContextFailsClosed() {
        when(merchantResolver.getCurrentMerchantAccess()).thenReturn(null);

        assertThatThrownBy(() -> guard.requireAnyRole("OWNER"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(403);
    }
}
