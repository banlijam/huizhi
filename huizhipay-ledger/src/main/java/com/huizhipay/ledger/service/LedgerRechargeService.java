package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.common.enums.AccountTypeEnum;
import com.huizhipay.ledger.entity.Account;
import com.huizhipay.ledger.entity.LedgerEntry;
import com.huizhipay.ledger.mapper.AccountMapper;
import com.huizhipay.ledger.mapper.LedgerEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerRechargeService {
    private final AccountMapper accountMapper;
    private final LedgerEntryMapper ledgerEntryMapper;

    @Transactional
    public void recharge(String merchantId, String currency, BigDecimal amount, String bizId, String channel, String externalOrderId) {
        // 1. 锁定并增加可用余额（乐观锁）
        Account assetAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                                                               .eq(Account::getMerchantId, merchantId)
                                                               .eq(Account::getAccountType, AccountTypeEnum.ASSET_AVAILABLE)
                                                               .eq(Account::getCurrency, currency));
        Account liabilityAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                                                                   .eq(Account::getMerchantId, merchantId)
                                                                   .eq(Account::getAccountType, AccountTypeEnum.LIABILITY_CUSTODY)
                                                                   .eq(Account::getCurrency, currency));
        BigDecimal assetBefore = assetAccount.getBalance();
        BigDecimal liabilityBefore = liabilityAccount.getBalance();

        BigDecimal assetAfter = assetBefore + amount;
        BigDecimal liabilityAfter = liabilityBefore + amount;

        assetAccount.setBalance(assetAfter);
        liabilityAccount.setBalance(liabilityAfter);

        int rows1 = accountMapper.updateById(assetAccount);
        int rows2 = accountMapper.updateById(liabilityAccount);

        if (rows1 != 1 || rows2 != 1) {
            throw new RuntimeException("充值并发冲突，请稍后重试");
        }

        // 5. 构造两条流水记录（一借一贷，金额都为正数，落地双记录）
        // 流水 1：资产账户入账（正数代表资产增加）
        LedgerEntry entryAsset = new LedgerEntry();
        entryAsset.setMerchantId(merchantId);
        entryAsset.setAccountNo(assetAccount.getAccountNo());
        entryAsset.setAmount(amount);                          // 正数：+500
        entryAsset.setBalanceBefore(assetBefore);
        entryAsset.setBalanceAfter(assetAfter);
        entryAsset.setBizType("RECHARGE");
        entryAsset.setBizId(bizId);
        entryAsset.setChannel(channel);
        entryAsset.setExternalOrderId(externalOrderId);
        entryAsset.setEntryStatus("CONFIRMED");
        entryAsset.setRemark("Airwallex法币充值入账（资产）");

        // 流水 2：负债账户入账（正数代表托管应付账款增加）
        LedgerEntry entryLiability = new LedgerEntry();
        entryLiability.setMerchantId(merchantId);
        entryLiability.setAccountNo(liabilityAccount.getAccountNo());
        entryLiability.setAmount(amount);                      // 正数：+500
        entryLiability.setBalanceBefore(liabilityBefore);
        entryLiability.setBalanceAfter(liabilityAfter);
        entryLiability.setBizType("RECHARGE");
        entryLiability.setBizId(bizId);
        entryLiability.setChannel(channel);
        entryLiability.setExternalOrderId(externalOrderId);
        entryLiability.setEntryStatus("CONFIRMED");
        entryLiability.setRemark("Airwallex法币充值入账（托管负债）");

        ledgerEntryMapper.insert(List.of(entryAsset, entryLiability));

        log.info("商户 {} 充值成功，金额 {}，业务号 {}，资产余额 {}，负债余额 {}",
                merchantId, amount, bizId, assetAfter, liabilityAfter);
    }
}