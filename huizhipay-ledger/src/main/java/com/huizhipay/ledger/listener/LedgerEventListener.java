package com.huizhipay.ledger.listener;

import com.huizhipay.common.event.PaymentAuthorizedEvent;
import com.huizhipay.ledger.entity.LedgerEntry;
import com.huizhipay.ledger.mapper.LedgerEntryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LedgerEventListener {
    private final LedgerEntryMapper repository;

    /**
     * 异步监听收单网关支付成功的信号
     */
    @Async
    @EventListener
    public void onPaymentAuthorized(PaymentAuthorizedEvent event) {
        System.out.println("[Ledger Module] Received authorized event for txn: " + event.merchantId());
        // 写入初始记账：将这笔 180 天后才能提现的 legacy_float 冻结在 PENDING 账户
        LedgerEntry entry = new LedgerEntry().setMerchantId(event.merchantId())
                //.transactionId(event.getTransactionId()).debitAccount("LEGACY_FLOAT")
                // 代表收单行卡组织池的源头
                //.creditAccount("PENDING")      // 商户冻结滚存账户
                //.amount(event.getAmount()).createTime(LocalDateTime.now()).build();
                ;
        repository.insertOrUpdate(entry);
    }
}