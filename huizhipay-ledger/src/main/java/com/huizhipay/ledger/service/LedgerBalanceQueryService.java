package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.common.port.BalanceQueryPort;
import com.huizhipay.ledger.entity.Account;
import com.huizhipay.ledger.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ledger-owned implementation of the balance query boundary.
 */
@Service
@RequiredArgsConstructor
public class LedgerBalanceQueryService implements BalanceQueryPort {
    private final AccountMapper accountMapper;

    @Override
    public BigDecimal getAvailableBalance(String merchantId) {
        if (merchantId == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        Account account = accountMapper.selectOne(
                Wrappers.<Account>lambdaQuery()
                        .eq(Account::getMerchantId, merchantId)
                        .eq(Account::getAccountType, Account.AccountTypeEnum.ASSET_AVAILABLE));
        if (account == null || account.getBalance() == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return account.getBalance().setScale(3, RoundingMode.HALF_UP);
    }
}
