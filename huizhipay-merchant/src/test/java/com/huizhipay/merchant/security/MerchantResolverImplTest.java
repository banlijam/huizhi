package com.huizhipay.merchant.security;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.merchant.entity.Merchant;
import com.huizhipay.merchant.entity.MerchantTeam;
import com.huizhipay.merchant.mapper.MerchantMapper;
import com.huizhipay.merchant.mapper.MerchantTeamMapper;
import com.huizhipay.user.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantResolverImplTest {
    @Mock private MerchantMapper merchantMapper;
    @Mock private MerchantTeamMapper merchantTeamMapper;
    private MerchantResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new MerchantResolverImpl(merchantMapper, merchantTeamMapper);
        UserPrincipal principal = new UserPrincipal(7L, "member@example.com", "", true, false, 1, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void ownerGetsOnlyOwnedMerchantContext() {
        when(merchantMapper.selectOne(any())).thenReturn(new Merchant().setMerchantId("M-A").setOwnerUserId(7L));
        assertThat(resolver.getCurrentMerchantAccess())
                .isEqualTo(new MerchantResolver.MerchantAccess("M-A", "OWNER"));
    }

    @Test
    void acceptedTeamMembershipCarriesItsMerchantAndRole() {
        when(merchantMapper.selectOne(any())).thenReturn(null);
        when(merchantTeamMapper.selectList(any())).thenReturn(List.of(
                new MerchantTeam().setMerchantId("M-B").setEmail("member@example.com")
                        .setRole(MerchantTeam.TeamRole.READONLY)
                        .setStatus(MerchantTeam.TeamInviteStatus.ACCEPTED)));
        assertThat(resolver.getCurrentMerchantAccess())
                .isEqualTo(new MerchantResolver.MerchantAccess("M-B", "READONLY"));
        @SuppressWarnings("unchecked") ArgumentCaptor<Wrapper<MerchantTeam>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(merchantTeamMapper).selectList(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("email", "status");
        AbstractWrapper<?, ?, ?> abstractQuery = (AbstractWrapper<?, ?, ?>) query.getValue();
        assertThat(abstractQuery.getParamNameValuePairs())
                .containsValue("member@example.com")
                .containsValue(MerchantTeam.TeamInviteStatus.ACCEPTED);
    }

    @Test
    void ambiguousOrMissingMembershipFailsClosed() {
        when(merchantMapper.selectOne(any())).thenReturn(null);
        when(merchantTeamMapper.selectList(any())).thenReturn(List.of(
                new MerchantTeam().setMerchantId("M-A").setRole(MerchantTeam.TeamRole.ADMIN),
                new MerchantTeam().setMerchantId("M-B").setRole(MerchantTeam.TeamRole.ADMIN)));
        assertThat(resolver.getCurrentMerchantAccess()).isNull();
    }
}
