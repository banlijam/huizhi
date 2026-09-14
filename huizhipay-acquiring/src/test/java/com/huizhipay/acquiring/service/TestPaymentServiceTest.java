package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import com.huizhipay.acquiring.transfi.checkout.TransFiCheckoutGateway;
import com.huizhipay.common.exceptions.BizException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestPaymentServiceTest {
    @Mock PaymentOrderMapper mapper;
    @Mock TransFiCheckoutGateway gateway;

    @BeforeAll static void metadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), PaymentOrder.class);
    }

    @Test void persistsBeforeCallingChannelAndReturnsRealGatewayUrl() {
        when(gateway.createInvoice(any())).thenAnswer(invocation -> {
            verify(mapper).insert(any(PaymentOrder.class));
            return new TransFiCheckoutGateway.Result("invoice-1", "https://checkout.transfi.test/pay/1");
        });
        TestPaymentService service = new TestPaymentService(mapper, gateway);
        var result = service.create("M-A", command("SHOP-1", "12.00"), "https://merchant.test");
        assertThat(result.paymentUrl()).isEqualTo("https://checkout.transfi.test/pay/1");
        assertThat(result.channelStatus()).isEqualTo("INITIATED");
    }

    @Test void sameMerchantOrderIsIdempotentButChangedAmountConflicts() {
        PaymentOrder existing = existing("M-A", "SHOP-1", "12.00");
        TestPaymentService service = new TestPaymentService(mapper, gateway);
        var initial = command("SHOP-1", "12.00");
        service.create("M-A", initial, "https://merchant.test");
        ArgumentCaptor<PaymentOrder> inserted = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(mapper).insert(inserted.capture());
        existing.setFingerprint(inserted.getValue().getFingerprint());
        when(mapper.selectOne(any())).thenReturn(existing);
        assertThat(service.create("M-A", initial, "https://merchant.test").platformOrderNo())
                .isEqualTo("TFI-EXISTING");
        assertThatThrownBy(() -> service.create("M-A", command("SHOP-1", "13.00"), "https://merchant.test"))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(409);
    }

    @Test void channelTimeoutLeavesPendingConfirmationAndNoInventedUrl() {
        when(gateway.createInvoice(any())).thenThrow(new IllegalStateException("timeout"));
        TestPaymentService service = new TestPaymentService(mapper, gateway);
        var result = service.create("M-A", command("SHOP-2", "12.00"), "https://merchant.test");
        assertThat(result.channelStatus()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(result.paymentUrl()).isNull();
    }

    @Test void rejectsTamperedAmountPrecisionAndUnapprovedRedirectOrigin() {
        TestPaymentService service = new TestPaymentService(mapper, gateway);
        assertThatThrownBy(() -> service.create("M-A", command("SHOP-3", "12.001"), "https://merchant.test"))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(400);
        var badUrl = new TestPaymentService.CreateCommand("SHOP-4", new BigDecimal("12.00"), "USD", "Mug",
                "https://evil.test/order", "https://merchant.test/fail");
        assertThatThrownBy(() -> service.create("M-A", badUrl, "https://merchant.test"))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(400);
    }

    @Test void queryAlwaysIncludesMerchantAndTransFiChannelScope() {
        when(mapper.selectOne(any())).thenReturn(existing("M-A", "SHOP-5", "12.00"));
        TestPaymentService service = new TestPaymentService(mapper, gateway);
        service.get("M-A", "SHOP-5");
        @SuppressWarnings("unchecked") ArgumentCaptor<Wrapper<PaymentOrder>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(query.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) query.getValue();
        assertThat(query.getValue().getSqlSegment()).contains("merchant_id", "merchant_order_no", "channel");
        assertThat(wrapper.getParamNameValuePairs()).containsValue("M-A").containsValue("SHOP-5")
                .containsValue(TestPaymentService.CHANNEL);
    }

    private TestPaymentService.CreateCommand command(String id, String amount) {
        return new TestPaymentService.CreateCommand(id, new BigDecimal(amount), "USD", "Mug",
                "https://merchant.test/orders/token?return=success", "https://merchant.test/orders/token?return=failed");
    }
    private PaymentOrder existing(String merchant, String merchantOrder, String amount) {
        return new PaymentOrder().setOrderNo("TFI-EXISTING").setMerchantId(merchant).setMerchantOrderNo(merchantOrder)
                .setAmount(new BigDecimal(amount)).setCurrency("USD").setChannel(TestPaymentService.CHANNEL)
                .setReturnUrl("https://merchant.test/orders/token?return=success")
                .setStatus(PaymentOrder.PaymentStatus.PENDING).setChannelStatus("INITIATED");
    }
}
