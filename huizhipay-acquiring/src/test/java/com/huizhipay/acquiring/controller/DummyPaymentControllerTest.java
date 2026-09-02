package com.huizhipay.acquiring.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import com.huizhipay.acquiring.service.DummyPaymentPolicy;
import com.huizhipay.acquiring.service.DummyPaymentCompletionService;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.common.security.MerchantResolver.MerchantAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DummyPaymentControllerTest {
    @Mock private PaymentOrderMapper paymentOrderMapper;
    @Mock private MerchantResolver merchantResolver;
    @Mock private MerchantAccessGuard merchantAccessGuard;
    @Mock private DummyPaymentPolicy dummyPaymentPolicy;
    @Mock private DummyPaymentCompletionService dummyPaymentCompletionService;
    @InjectMocks private DummyPaymentController controller;

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), PaymentOrder.class);
    }

    @Test
    void createAlwaysUsesMerchantFromAuthenticatedContext() {
        when(merchantAccessGuard.requireAnyRole("OWNER", "ADMIN"))
                .thenReturn(new MerchantAccess("M-A", "ADMIN"));
        controller.create(new DummyPaymentController.CreateOrderRequest(
                new BigDecimal("10.00"), "USD", "/merchant"));
        ArgumentCaptor<PaymentOrder> orderCaptor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getMerchantId()).isEqualTo("M-A");
    }

    @Test
    void createRejectsCurrenciesWithoutSandboxLedgerAccounts() {
        when(merchantAccessGuard.requireAnyRole("OWNER", "ADMIN"))
                .thenReturn(new MerchantAccess("M-A", "ADMIN"));

        assertThatThrownBy(() -> controller.create(new DummyPaymentController.CreateOrderRequest(
                new BigDecimal("10.00"), "HKD", "/merchant")))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(400);
        verifyNoInteractions(paymentOrderMapper);
    }

    @Test
    void listScopesEveryQueryToAuthenticatedMerchant() {
        when(merchantResolver.getCurrentMerchantId()).thenReturn("M-A");
        when(paymentOrderMapper.selectCount(any())).thenReturn(0L);
        when(paymentOrderMapper.selectList(any())).thenReturn(List.of());
        controller.list(1);
        @SuppressWarnings("unchecked") ArgumentCaptor<Wrapper<PaymentOrder>> countQuery = ArgumentCaptor.forClass(Wrapper.class);
        @SuppressWarnings("unchecked") ArgumentCaptor<Wrapper<PaymentOrder>> listQuery = ArgumentCaptor.forClass(Wrapper.class);
        verify(paymentOrderMapper).selectCount(countQuery.capture());
        verify(paymentOrderMapper).selectList(listQuery.capture());
        assertMerchantScope(countQuery.getValue());
        assertMerchantScope(listQuery.getValue());
    }

    @Test
    void merchantlessUserCannotCreateOrListOrders() {
        doThrow(new BizException(403, "Forbidden"))
                .when(merchantAccessGuard).requireAnyRole("OWNER", "ADMIN");
        when(merchantResolver.getCurrentMerchantId()).thenReturn(null);
        assertThatThrownBy(() -> controller.create(new DummyPaymentController.CreateOrderRequest(
                BigDecimal.ONE, "USD", "/merchant")))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(403);
        assertThatThrownBy(() -> controller.list(1))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(403);
    }

    @Test
    void disabledBrowserResultFailsBeforeReadingOrUpdatingAnOrder() {
        doThrow(new BizException(403, "Disabled"))
                .when(dummyPaymentPolicy).requireBrowserResultSubmission();

        assertThatThrownBy(() -> controller.result("ct_public", new DummyPaymentController.ResultRequest("SUCCESS")))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(403);
        verifyNoInteractions(paymentOrderMapper, dummyPaymentCompletionService);
    }

    @Test
    void localSandboxCanStillSimulateAResultWhenExplicitlyEnabled() {
        PaymentOrder order = new PaymentOrder()
                .setCheckoutToken("ct_local")
                .setOrderNo("DUMMY-1")
                .setMerchantId("M-A")
                .setAmount(BigDecimal.ONE)
                .setCurrency("USD")
                .setStatus(PaymentOrder.PaymentStatus.PENDING);
        when(dummyPaymentCompletionService.complete("ct_local", "SUCCESS")).thenReturn(order);

        controller.result("ct_local", new DummyPaymentController.ResultRequest("SUCCESS"));

        verify(dummyPaymentCompletionService).complete("ct_local", "SUCCESS");
    }

    private void assertMerchantScope(Wrapper<PaymentOrder> query) {
        assertThat(query.getSqlSegment()).contains("merchant_id", "channel");
        AbstractWrapper<?, ?, ?> abstractQuery = (AbstractWrapper<?, ?, ?>) query;
        assertThat(abstractQuery.getParamNameValuePairs()).containsValue("M-A").containsValue("DUMMY");
    }
}
