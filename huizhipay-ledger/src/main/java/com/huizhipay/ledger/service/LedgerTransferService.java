package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.ledger.entity.Account;
import com.huizhipay.ledger.entity.Account.AccountTypeEnum;
import com.huizhipay.ledger.entity.AccountEntryDetail;
import com.huizhipay.ledger.entity.LedgerEntry.BizTypeEnum;
import com.huizhipay.ledger.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static manifold.science.util.CoercionConstants.bd;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerTransferService {

    private final AccountMapper accountMapper;
    private final LedgerBookingService ledgerBookingService;

    private static final String PLATFORM_MERCHANT_ID = "__PLATFORM__";
    private static final BigDecimal FEE_RATE = 0.07bd;

    @Transactional
    public void payment(String merchantId, String currency, BigDecimal amount,
                        String bizId, String channel, String externalOrderId) {
        Account assetAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getMerchantId, merchantId)
                .eq(Account::getAccountType, AccountTypeEnum.ASSET_AVAILABLE)
                .eq(Account::getCurrency, currency));

        Account liabilityAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getMerchantId, merchantId)
                .eq(Account::getAccountType, AccountTypeEnum.LIABILITY_CUSTODY)
                .eq(Account::getCurrency, currency));

        Account platformIncomeAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getMerchantId, PLATFORM_MERCHANT_ID)
                .eq(Account::getAccountType, AccountTypeEnum.PLATFORM_INCOME)
                .eq(Account::getCurrency, currency));

        BigDecimal merchantNet = amount * (1bd - FEE_RATE);
        BigDecimal platformFee = amount * FEE_RATE;

        AccountEntryDetail merchantDetail = new AccountEntryDetail()
                .setAccountNo(assetAccount.getAccountNo())
                .setMerchantId(merchantId)
                .setAmount(merchantNet)
                .setBizType(BizTypeEnum.PAYMENT)
                .setChannel(channel)
                .setExternalOrderId(externalOrderId)
                .setRemark("充值入账（净额 " + merchantNet + "）");

        AccountEntryDetail platformDetail = new AccountEntryDetail()
                .setAccountNo(platformIncomeAccount.getAccountNo())
                .setMerchantId(PLATFORM_MERCHANT_ID)
                .setAmount(platformFee)
                .setBizType(BizTypeEnum.PAYMENT)
                .setChannel(channel)
                .setExternalOrderId(externalOrderId)
                .setRemark("充值手续费 7%（" + platformFee + "）");

        AccountEntryDetail liabilityDetail = new AccountEntryDetail()
                .setAccountNo(liabilityAccount.getAccountNo())
                .setMerchantId(merchantId)
                .setAmount(-amount)
                .setBizType(BizTypeEnum.PAYMENT)
                .setChannel(channel)
                .setExternalOrderId(externalOrderId)
                .setRemark("充值托管负债（全额 " + amount + "）");

        ledgerBookingService.doubleEntryBooking(
                merchantId,
                bizId,
                List.of(merchantDetail, platformDetail, liabilityDetail)
        );

        log.info("商户 {} 充值成功，金额 {}，业务号 {}，商户净得 {}，平台手续费 {}",
                merchantId, amount, bizId, merchantNet, platformFee);
    }

    @Transactional
    public void withdraw(String merchantId, String currency, BigDecimal amount,
                         String bizId, String channel, String externalOrderId) {
        Account assetAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getMerchantId, merchantId)
                .eq(Account::getAccountType, AccountTypeEnum.ASSET_AVAILABLE)
                .eq(Account::getCurrency, currency));

        Account liabilityAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getMerchantId, merchantId)
                .eq(Account::getAccountType, AccountTypeEnum.LIABILITY_CUSTODY)
                .eq(Account::getCurrency, currency));

        AccountEntryDetail assetDetail = new AccountEntryDetail()
                .setAccountNo(assetAccount.getAccountNo())
                .setMerchantId(merchantId)
                .setAmount(-amount)
                .setBizType(BizTypeEnum.WITHDRAWAL)
                .setChannel(channel)
                .setExternalOrderId(externalOrderId)
                .setRemark("商户提现（金额 " + amount + "）");

        AccountEntryDetail liabilityDetail = new AccountEntryDetail()
                .setAccountNo(liabilityAccount.getAccountNo())
                .setMerchantId(merchantId)
                .setAmount(amount)
                .setBizType(BizTypeEnum.WITHDRAWAL)
                .setChannel(channel)
                .setExternalOrderId(externalOrderId)
                .setRemark("托管负债减少（金额 " + amount + "）");

        ledgerBookingService.doubleEntryBooking(
                merchantId,
                bizId,
                List.of(assetDetail, liabilityDetail)
        );

        log.info("商户 {} 提现成功，金额 {}，业务号 {}",
                merchantId, amount, bizId);
    }
}
