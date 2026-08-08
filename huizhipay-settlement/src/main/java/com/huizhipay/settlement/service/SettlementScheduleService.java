package com.huizhipay.settlement.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.settlement.dto.SettlementCountdown;
import com.huizhipay.settlement.entity.SettlementSchedule;
import com.huizhipay.settlement.entity.SettlementSchedule.SettlementStatusEnum;
import com.huizhipay.settlement.mapper.SettlementScheduleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * T+1 清算计划服务，提供清算倒计时。
 * 被 huizhipay-ledger 的指挥中心聚合调用（ledger → settlement，单向依赖）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementScheduleService {

    private static final int SETTLEMENT_CYCLE_HOURS = 24;

    private final SettlementScheduleMapper settlementScheduleMapper;

    /**
     * 获取距离下一笔 T+1 清算的倒计时。
     * 若无清算计划记录，默认以"下一 UTC 午夜"为预计到账时间。
     */
    public SettlementCountdown getNextSettlement(String merchantId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expectedAt;

        if (merchantId != null) {
            SettlementSchedule schedule = settlementScheduleMapper.selectOne(
                    new QueryWrapper<SettlementSchedule>()
                            .eq("merchant_id", merchantId)
                            .eq("status", SettlementStatusEnum.PENDING.name())
                            .orderByAsc("expected_at")
                            .last("limit 1"));
            if (schedule != null && schedule.getExpectedAt() != null) {
                expectedAt = schedule.getExpectedAt();
                log.debug("[Settlement] 使用数据库中pending清算计划 merchantId={}, expectedAt={}, gross={}, net={}",
                        merchantId, expectedAt, schedule.getGrossAmount(), schedule.getNetAmount());
            } else {
                expectedAt = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay();
                log.debug("[Settlement] 无pending清算计划，默认下一UTC午夜 merchantId={}, expectedAt={}",
                        merchantId, expectedAt);
            }
        } else {
            expectedAt = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay();
            log.debug("[Settlement] merchantId为空，默认下一UTC午夜 expectedAt={}", expectedAt);
        }

        long minutes = Duration.between(now, expectedAt).toMinutes();
        double hoursRemaining = Math.max(0, minutes / 60.0);
        // 保留 1 位小数
        hoursRemaining = Math.round(hoursRemaining * 10.0) / 10.0;
        log.info("[Settlement] 倒计时计算 merchantId={}, 剩余{}小时, 预计到账={}", merchantId, hoursRemaining, expectedAt);
        return new SettlementCountdown(hoursRemaining, SETTLEMENT_CYCLE_HOURS, expectedAt);
    }
}
