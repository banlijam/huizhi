package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.acquiring.entity.PaymentEventLog;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentEventLogMapper;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.ledger.service.LedgerTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DummyPaymentCompletionService {
    private static final String DUMMY_CHANNEL = "DUMMY";

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentEventLogMapper paymentEventLogMapper;
    private final LedgerTransferService ledgerTransferService;

    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder complete(String checkoutToken, String requestedResultValue) {
        String requestedResult = requestedResultValue == null
                ? "" : requestedResultValue.trim().toUpperCase(Locale.ROOT);
        if (!"SUCCESS".equals(requestedResult) && !"FAILED".equals(requestedResult)) {
            throw new BizException(400, "Dummy result must be SUCCESS or FAILED");
        }

        PaymentOrder order = requireOrder(checkoutToken);
        if (order.getStatus() != PaymentOrder.PaymentStatus.PENDING) {
            return order;
        }

        PaymentOrder.PaymentStatus status = PaymentOrder.PaymentStatus.valueOf(requestedResult);
        String transactionId = "DUMMY_TXN_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase(Locale.ROOT);
        LocalDateTime completedAt = LocalDateTime.now();
        String remark = status == PaymentOrder.PaymentStatus.SUCCESS
                ? "Dummy payment succeeded" : "Dummy payment failed";

        int updated = paymentOrderMapper.update(null,
                Wrappers.<PaymentOrder>lambdaUpdate()
                        .eq(PaymentOrder::getCheckoutToken, checkoutToken)
                        .eq(PaymentOrder::getChannel, DUMMY_CHANNEL)
                        .eq(PaymentOrder::getStatus, PaymentOrder.PaymentStatus.PENDING)
                        .set(PaymentOrder::getStatus, status)
                        .set(PaymentOrder::getChannelTradeNo, transactionId)
                        .set(PaymentOrder::getRemark, remark)
                        .set(PaymentOrder::getUpdatedAt, completedAt));
        if (updated != 1) {
            return requireOrder(checkoutToken);
        }

        order.setStatus(status)
                .setChannelTradeNo(transactionId)
                .setRemark(remark)
                .setUpdatedAt(completedAt);

        if (status == PaymentOrder.PaymentStatus.SUCCESS) {
            ledgerTransferService.payment(order.getMerchantId(), order.getCurrency(), order.getAmount(),
                    order.getOrderNo(), DUMMY_CHANNEL, transactionId);
        }

        paymentEventLogMapper.insert(new PaymentEventLog()
                .setOrderNo(order.getOrderNo())
                .setMerchantId(order.getMerchantId())
                .setEventType(status == PaymentOrder.PaymentStatus.SUCCESS
                        ? "payment.succeeded" : "payment.failed")
                .setTransactionId(transactionId)
                .setCreatedAt(completedAt));
        return order;
    }

    private PaymentOrder requireOrder(String checkoutToken) {
        PaymentOrder order = paymentOrderMapper.selectOne(
                Wrappers.<PaymentOrder>lambdaQuery()
                        .eq(PaymentOrder::getCheckoutToken, checkoutToken)
                        .eq(PaymentOrder::getChannel, DUMMY_CHANNEL));
        if (order == null) {
            throw new BizException(404, "Dummy checkout token not found");
        }
        return order;
    }
}
