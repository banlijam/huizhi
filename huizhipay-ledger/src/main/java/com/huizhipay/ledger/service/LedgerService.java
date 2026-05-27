package com.huizhipay.ledger.service;

import com.huizhipay.ledger.entity.LedgerEntry;
import com.huizhipay.ledger.mapper.LedgerEntryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryMapper ledgerRepository;

    @Transactional(rollbackFor = Exception.class)
    public void recordDoubleEntry(String merchantId, String amount, String bizType) {
        // 借方记录（资产增加 / 收入）
        LedgerEntry debit = new LedgerEntry();
        debit.setMerchantId(merchantId);
        debit.setAmount(new BigDecimal(amount));
        // 贷方记录（资产减少 / 支出），需结合账户余额状态机
        LedgerEntry credit = new LedgerEntry();
        credit.setMerchantId(merchantId);
        credit.setAmount(new BigDecimal(amount));

        // 批量插入保证事务一致性
        ledgerRepository.insertOrUpdate(List.of(debit, credit));
        // 触发账户余额重算事件
    }
}