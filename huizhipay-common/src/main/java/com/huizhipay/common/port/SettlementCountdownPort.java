package com.huizhipay.common.port;

import java.time.LocalDateTime;

/**
 * Read-only boundary used by cross-domain overview queries.
 */
public interface SettlementCountdownPort {
    SettlementCountdownSnapshot getNextSettlement(String merchantId);

    record SettlementCountdownSnapshot(double hoursRemaining, int totalHours, LocalDateTime expectedAt) {
    }
}
