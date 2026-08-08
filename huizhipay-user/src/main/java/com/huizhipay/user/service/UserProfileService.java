package com.huizhipay.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.ledger.entity.Account;
import com.huizhipay.ledger.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 商户头部资料服务：可用余额取自 ledger 的 ASSET_AVAILABLE 账户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AccountMapper accountMapper;

    public BigDecimal getBalance(String merchantId) {
        log.debug("[UserProfile] 查询可用余额 merchantId={}", merchantId);
        if (merchantId == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        Account account = accountMapper.selectOne(
                new QueryWrapper<Account>()
                        .eq("merchant_id", merchantId)
                        .eq("account_type", "ASSET_AVAILABLE"));
        if (account == null || account.getBalance() == null) {
            log.debug("[UserProfile] 账户不存在或余额为空 merchantId={}, 返回0", merchantId);
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal balance = account.getBalance().setScale(3, RoundingMode.HALF_UP);
        log.debug("[UserProfile] 余额查询成功 merchantId={}, balance={}", merchantId, balance);
        return balance;
    }
}
