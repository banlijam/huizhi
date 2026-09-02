package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.huizhipay.acquiring.entity.PaymentEventLog;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentEventLogMapper;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.ledger.service.LedgerTransferService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DummyPaymentCompletionServiceTest {
    @Mock private PaymentOrderMapper paymentOrderMapper;
    @Mock private PaymentEventLogMapper paymentEventLogMapper;
    @Mock private LedgerTransferService ledgerTransferService;
    @InjectMocks private DummyPaymentCompletionService service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), PaymentOrder.class);
    }

    @Test
    void successUpdatesOnceBooksLedgerAndRecordsEvent() {
        PaymentOrder order = pendingOrder("ct_success", "DUMMY-100");
        when(paymentOrderMapper.selectOne(any())).thenReturn(order);
        when(paymentOrderMapper.update(any(), any())).thenReturn(1);

        PaymentOrder completed = service.complete("ct_success", "SUCCESS");

        assertThat(completed.getStatus()).isEqualTo(PaymentOrder.PaymentStatus.SUCCESS);
        assertThat(completed.getChannelTradeNo()).startsWith("DUMMY_TXN_");
        verify(ledgerTransferService).payment("M-A", "USD", new BigDecimal("100.000"),
                "DUMMY-100", "DUMMY", completed.getChannelTradeNo());
        ArgumentCaptor<PaymentEventLog> event = ArgumentCaptor.forClass(PaymentEventLog.class);
        verify(paymentEventLogMapper).insert(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("payment.succeeded");
        assertThat(event.getValue().getOrderNo()).isEqualTo("DUMMY-100");
    }

    @Test
    void failureRecordsEventWithoutLedgerBooking() {
        PaymentOrder order = pendingOrder("ct_failed", "DUMMY-FAIL");
        when(paymentOrderMapper.selectOne(any())).thenReturn(order);
        when(paymentOrderMapper.update(any(), any())).thenReturn(1);

        PaymentOrder completed = service.complete("ct_failed", "FAILED");

        assertThat(completed.getStatus()).isEqualTo(PaymentOrder.PaymentStatus.FAILED);
        verifyNoInteractions(ledgerTransferService);
        ArgumentCaptor<PaymentEventLog> event = ArgumentCaptor.forClass(PaymentEventLog.class);
        verify(paymentEventLogMapper).insert(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("payment.failed");
    }

    @Test
    void replayOfTerminalOrderDoesNotWriteAgain() {
        PaymentOrder order = pendingOrder("ct_replay", "DUMMY-REPLAY")
                .setStatus(PaymentOrder.PaymentStatus.SUCCESS)
                .setChannelTradeNo("DUMMY_TXN_EXISTING");
        when(paymentOrderMapper.selectOne(any())).thenReturn(order);

        PaymentOrder replay = service.complete("ct_replay", "SUCCESS");

        assertThat(replay.getChannelTradeNo()).isEqualTo("DUMMY_TXN_EXISTING");
        verify(paymentOrderMapper, never()).update(any(), any());
        verifyNoInteractions(ledgerTransferService, paymentEventLogMapper);
    }

    @Test
    void losingAConcurrentTerminalUpdateReturnsTheCommittedOrder() {
        PaymentOrder pending = pendingOrder("ct_race", "DUMMY-RACE");
        PaymentOrder committed = pendingOrder("ct_race", "DUMMY-RACE")
                .setStatus(PaymentOrder.PaymentStatus.SUCCESS)
                .setChannelTradeNo("DUMMY_TXN_WINNER");
        when(paymentOrderMapper.selectOne(any())).thenReturn(pending, committed);
        when(paymentOrderMapper.update(any(), any())).thenReturn(0);

        PaymentOrder result = service.complete("ct_race", "SUCCESS");

        assertThat(result.getChannelTradeNo()).isEqualTo("DUMMY_TXN_WINNER");
        verifyNoInteractions(ledgerTransferService, paymentEventLogMapper);
    }

    @Test
    void invalidResultIsRejectedBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service.complete("ct_invalid", "maybe"))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(400);
        verifyNoInteractions(paymentOrderMapper, ledgerTransferService, paymentEventLogMapper);
    }

    private PaymentOrder pendingOrder(String checkoutToken, String orderNo) {
        return new PaymentOrder()
                .setCheckoutToken(checkoutToken)
                .setOrderNo(orderNo)
                .setMerchantId("M-A")
                .setAmount(new BigDecimal("100.000"))
                .setCurrency("USD")
                .setChannel("DUMMY")
                .setStatus(PaymentOrder.PaymentStatus.PENDING);
    }
}
