package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentOrderExpiryService {
    private static final long PAYMENT_TIMEOUT_MINUTES = 30;
    private static final List<String> CHECKOUT_CHANNELS = List.of("DUMMY", TestPaymentService.CHANNEL);

    private final PaymentOrderMapper paymentOrderMapper;

    @Scheduled(fixedDelayString = "${huizhipay.payments.expiry-scan-delay-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public int expirePendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        return paymentOrderMapper.update(null,
                Wrappers.<PaymentOrder>lambdaUpdate()
                        .eq(PaymentOrder::getStatus, PaymentOrder.PaymentStatus.PENDING)
                        .in(PaymentOrder::getChannel, CHECKOUT_CHANNELS)
                        .and(expired -> expired
                                .le(PaymentOrder::getExpireAt, now)
                                .or()
                                .isNull(PaymentOrder::getExpireAt)
                                .le(PaymentOrder::getCreatedAt, now.minusMinutes(PAYMENT_TIMEOUT_MINUTES)))
                        .set(PaymentOrder::getStatus, PaymentOrder.PaymentStatus.FAILED)
                        .set(PaymentOrder::getChannelStatus, "EXPIRED")
                        .set(PaymentOrder::getRemark, "Payment expired after 30 minutes")
                        .set(PaymentOrder::getUpdatedAt, now));
    }
}
