package com.huizhipay.settlement.service;

import com.huizhipay.common.port.SettlementCountdownPort;
import com.huizhipay.settlement.dto.SettlementCountdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Keeps settlement implementation types behind a small cross-module query boundary.
 */
@Component
@RequiredArgsConstructor
public class SettlementCountdownAdapter implements SettlementCountdownPort {
    private final SettlementScheduleService settlementScheduleService;

    @Override
    public SettlementCountdownSnapshot getNextSettlement(String merchantId) {
        SettlementCountdown countdown = settlementScheduleService.getNextSettlement(merchantId);
        return new SettlementCountdownSnapshot(
                countdown.getHoursRemaining(),
                countdown.getTotalHours(),
                countdown.getExpectedAt());
    }
}
