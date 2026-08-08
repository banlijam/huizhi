package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.ledger.entity.Account;
import com.huizhipay.ledger.entity.AccountEntryDetail;
import com.huizhipay.ledger.entity.LedgerEntry;
import com.huizhipay.ledger.entity.LedgerEntry.BizTypeEnum;
import com.huizhipay.ledger.mapper.AccountMapper;
import com.huizhipay.ledger.mapper.LedgerEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.math.BigDecimal.ZERO;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerBookingService {

    private final AccountMapper accountMapper;
    private final LedgerEntryMapper ledgerEntryMapper;

    @Transactional(rollbackFor = Exception.class)
    public void doubleEntryBooking(String merchantId,
                                   String bizId,
                                   List<AccountEntryDetail> details) {
        log.info("[Ledger] 开始复式记账 merchantId={}, bizId={}, 明细条数={}", merchantId, bizId, details.size());

        BigDecimal total = ZERO;
        for (AccountEntryDetail detail : details) {
            total = total.add(detail.getAmount());
        }
        if (total != ZERO) {
            log.error("[Ledger] 复式记账借贷不平衡 merchantId={}, bizId={}, total={}", merchantId, bizId, total);
            throw new BizException(500, "复式记账借贷不平衡，总和：" + total);
        }
        log.debug("[Ledger] 借贷平衡校验通过，合计={}", total);

        List<LedgerEntry> entries = new ArrayList<>();
        List<Account> accountsToUpdate = new ArrayList<>();

        for (AccountEntryDetail detail : details) {
            Account account = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                    .eq(Account::getAccountNo, detail.getAccountNo()));
            log.debug("[Ledger] 处理账户 accountNo={}, amount={}", detail.getAccountNo(), detail.getAmount());

            BigDecimal beforeBalance = account.getBalance();
            BigDecimal afterBalance = beforeBalance + detail.getAmount();

            if (detail.getAmount() < ZERO && beforeBalance < detail.getAmount().abs()) {
                log.warn("[Ledger] 账户余额不足 accountNo={}, before={}, 需扣减={}",
                        detail.getAccountNo(), beforeBalance, detail.getAmount().abs());
                throw new BizException(400, "账户余额不足，账户号：" + detail.getAccountNo());
            }

            account.setBalance(afterBalance);
            account.setVersion(account.getVersion() + 1);
            accountsToUpdate.add(account);

            entries.add(new LedgerEntry()
                    .setMerchantId(detail.getMerchantId() != null ? detail.getMerchantId() : merchantId)
                    .setAccountNo(detail.getAccountNo())
                    .setAmount(detail.getAmount())
                    .setBalanceBefore(beforeBalance)
                    .setBalanceAfter(afterBalance)
                    .setBizType(detail.getBizType())
                    .setBizId(bizId)
                    .setChannel(detail.getChannel())
                    .setExternalOrderId(detail.getExternalOrderId())
                    .setEntryStatus(LedgerEntry.EntryStatusEnum.SETTLED)
                    .setRemark(detail.getRemark()));
        }

        for (Account account : accountsToUpdate) {
            int rows = accountMapper.updateById(account);
            if (rows != 1) {
                log.error("[Ledger] 乐观锁冲突 accountNo={}, version={}", account.getAccountNo(), account.getVersion());
                throw new BizException(500, "账户更新乐观锁冲突，请稍后重试");
            }
        }
        log.debug("[Ledger] 批量更新账户完成，共{}条", accountsToUpdate.size());

        ledgerEntryMapper.insert(entries);
        log.info("[Ledger] 复式记账完成 merchantId={}, bizId={}, 流水条数={}", merchantId, bizId, entries.size());
    }
}
