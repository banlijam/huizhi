package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.ledger.entity.Account;
import com.huizhipay.ledger.entity.LedgerEntry;
import com.huizhipay.ledger.mapper.AccountMapper;
import com.huizhipay.ledger.mapper.LedgerEntryMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.math.BigDecimal.ZERO;

@Service
@RequiredArgsConstructor
public class LedgerBatchService {

    private final AccountMapper accountMapper;
    private final LedgerEntryMapper ledgerEntryMapper;

    /**
     * 批量复式记账（双记账标准）
     *
     * @param merchantId        用户商户ID
     * @param platformAccountNo 平台收入账户号（用于归集手续费和成本）
     * @param bizId             业务流水号（如查询单号 Q123）
     * @param details           记账明细列表（金额正负已区分）
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchRecordWithDoubleEntry(String merchantId,
                                           String platformAccountNo,
                                           String bizId,
                                           List<AccountEntryDetail> details) {
        // 1. 先校验所有流水金额总和是否为 0（借贷必相等）
        BigDecimal total = details.stream()
                                  .map(AccountEntryDetail::getAmount)
                                  .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) != 0) {
            throw new BizException(500, "复式记账借贷不平衡，总和：" + total);
        }

        // 2. 准备批量更新的账户和流水
        List<LedgerEntry> entries = new ArrayList<>();
        List<Account> accountsToUpdate = new ArrayList<>();

        for (AccountEntryDetail detail : details) {
            // 2.1 查询并锁定账户（按 accountNo 排序防止死锁）
            Account account = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                                                              .eq(Account::getMerchantId, detail.accountNo));

            BigDecimal beforeBalance = account.getBalance();
            BigDecimal afterBalance = beforeBalance.add(detail.getAmount());

            // 余额不足检查（仅对用户账户扣减时校验）
            if (detail.getAmount() < ZERO && beforeBalance < detail.getAmount().abs()) {
                throw new BizException(400, "账户余额不足，账户号：" + detail.getAccountNo());
            }

            // 2.2 更新账户余额（使用乐观锁 version）
            account.setBalance(afterBalance);
            account.setVersion(account.getVersion() + 1);
            accountsToUpdate.add(account);

            // 2.3 构建流水明细
            entries.add(new LedgerEntry()
                    .setMerchantId(detail.getMerchantId() != null ? detail.getMerchantId() : merchantId)
                    .setAccountNo(detail.getAccountNo())
                    .setAmount(detail.getAmount())          // 正数入账，负数出账
                    .setBalanceBefore(beforeBalance)
                    .setBalanceAfter(afterBalance)
                    .setBizType(detail.getBizType())        // 如 QUERY_FEE, PLATFORM_INCOME
                    .setBizId(bizId)
                    .setChannel(detail.getChannel())        // 如 EXTERNAL_API
                    .setExternalOrderId(detail.getExternalOrderId())
                    .setEntryStatus("CONFIRMED")
                    .setRemark(detail.getRemark()));
        }

        // 3. 批量更新账户（防止脏写）
        for (Account account : accountsToUpdate) {
            int rows = accountMapper.updateById(account); // MyBatis-Plus 自动带 version 乐观锁
            if (rows != 1) {
                throw new BizException(500, "账户更新乐观锁冲突，请稍后重试");
            }
        }

        // 4. 批量插入流水
        ledgerEntryMapper.insert(entries);
    }

    // 内部辅助类（定义在 LedgerBatchService 同级目录）
    @Data
    @Accessors(chain = true)
    public static class AccountEntryDetail {
        private String accountNo;
        private String merchantId;      // 可为空，默认使用主商户ID
        private BigDecimal amount;      // 正=增加，负=扣减
        private String bizType;         // 业务类型
        private String channel;         // 渠道
        private String externalOrderId; // 外部单号
        private String remark;          // 备注
    }
}