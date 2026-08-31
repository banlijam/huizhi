package com.huizhipay.user.service;

import com.huizhipay.common.port.BalanceQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 商户头部资料服务：可用余额取自 ledger 的 ASSET_AVAILABLE 账户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final BalanceQueryPort balanceQueryPort;

    public BigDecimal getBalance(String merchantId) {
        log.debug("[UserProfile] 查询可用余额 merchantId={}", merchantId);
        BigDecimal balance = balanceQueryPort.getAvailableBalance(merchantId);
        log.debug("[UserProfile] 余额查询成功 merchantId={}, balance={}", merchantId, balance);
        return balance;
    }
}
