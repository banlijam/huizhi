package com.huizhipay.common.port;

import java.math.BigDecimal;

/**
 * Read-only boundary for querying a Merchant's available balance.
 *
 * <p>Callers depend on this contract instead of importing ledger entities or mappers.</p>
 */
public interface BalanceQueryPort {
    BigDecimal getAvailableBalance(String merchantId);
}
