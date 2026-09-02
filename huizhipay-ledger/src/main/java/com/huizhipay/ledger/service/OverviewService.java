package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.common.port.SettlementCountdownPort;
import com.huizhipay.common.port.SettlementCountdownPort.SettlementCountdownSnapshot;
import com.huizhipay.ledger.dto.*;
import com.huizhipay.ledger.entity.LedgerEntry;
import com.huizhipay.ledger.mapper.LedgerEntryMapper;
import com.huizhipay.ledger.mapper.OverviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指挥中心聚合服务：今日大盘 + 透明分账账本 + 清算倒计时（取自 settlement）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverviewService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.07");
    private static final BigDecimal NET_RATE = new BigDecimal("0.93");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final OverviewMapper overviewMapper;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final SettlementCountdownPort settlementCountdownPort;

    public OverviewStatsResponse getStats(String merchantId) {
        log.info("[Overview] 拉取指挥中心大盘 merchantId={}", merchantId);

        OverviewStatsResponse resp = new OverviewStatsResponse();
        OverviewStatsResponse.SplitRatio split = new OverviewStatsResponse.SplitRatio();
        split.setFeeRate(FEE_RATE);
        split.setNetRate(NET_RATE);
        split.setFeeLabel("7");
        split.setNetLabel("93");
        resp.setSplitRatio(split);

        SettlementCountdownSnapshot countdown = settlementCountdownPort.getNextSettlement(merchantId);
        log.debug("[Overview] 清算倒计时 merchantId={}, 剩余{}小时/总{}小时, 预计到账={}",
                merchantId, countdown.hoursRemaining(), countdown.totalHours(), countdown.expectedAt());
        resp.setSettlementCountdownHours(countdown.hoursRemaining());
        resp.setSettlementCountdownTotal(countdown.totalHours());

        if (merchantId == null) {
            log.debug("[Overview] 商户尚未入驻，返回空大盘");
            resp.setTodayCount(0);
            resp.setTodayVolume(BigDecimal.ZERO.setScale(3));
            resp.setChartData(emptyChart());
            return resp;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime yestStart = today.minusDays(1).atStartOfDay();

        OverviewRangeStat todayStat = defaultIfNull(overviewMapper.statsForRange(merchantId, todayStart, todayEnd));
        OverviewRangeStat yestStat = defaultIfNull(overviewMapper.statsForRange(merchantId, yestStart, todayStart));

        long todayCount = val(todayStat.getSuccessCount());
        long yestCount = val(yestStat.getSuccessCount());
        resp.setTodayCount(todayCount);
        resp.setTodayCountChange(pctChange(todayCount, yestCount));

        double todayRate = rate(todayStat);
        double yestRate = rate(yestStat);
        resp.setConversionRate(round1(todayRate));
        resp.setConversionRateChange(round1(pctChange(todayRate, yestRate)));

        BigDecimal todayVolume = todayStat.getSuccessVolume() == null ? BigDecimal.ZERO : todayStat.getSuccessVolume();
        BigDecimal yestVolume = yestStat.getSuccessVolume() == null ? BigDecimal.ZERO : yestStat.getSuccessVolume();
        resp.setTodayVolume(todayVolume.setScale(3, RoundingMode.HALF_UP));
        resp.setTodayVolumeChange(pctChangeBd(todayVolume, yestVolume));

        resp.setChartData(buildChart(merchantId, today));
        log.info("[Overview] 大盘返回 merchantId={}, 今日笔数={}, 流水={}, 转化率={}%",
                merchantId, todayCount, todayVolume, round1(todayRate));
        return resp;
    }

    public List<LedgerRowResponse> getLedger(String merchantId) {
        if (merchantId == null) {
            return List.of();
        }
        log.debug("[Overview] 拉取透明分账账本 merchantId={}", merchantId);
        List<LedgerEntry> entries = ledgerEntryMapper.selectList(
                new QueryWrapper<LedgerEntry>()
                        .eq("merchant_id", merchantId)
                        .eq("biz_type", "PAYMENT")
                        .lt("amount", BigDecimal.ZERO)
                        .orderByDesc("created_at")
                        .last("limit 10"));
        List<LedgerRowResponse> rows = new ArrayList<>(entries.size());
        for (LedgerEntry e : entries) {
            LedgerRowResponse row = new LedgerRowResponse();
            row.setOrderId(e.getBizId());
            BigDecimal gross = e.getAmount() == null ? BigDecimal.ZERO : e.getAmount().abs();
            row.setGross(gross.setScale(3, RoundingMode.HALF_UP));
            row.setFee(gross.multiply(FEE_RATE).setScale(3, RoundingMode.HALF_UP));
            row.setNet(gross.multiply(NET_RATE).setScale(3, RoundingMode.HALF_UP));
            row.setStatus(e.getEntryStatus().name());
            row.setTime(e.getCreatedAt());
            rows.add(row);
        }
        log.debug("[Overview] 账本返回{}条记录 merchantId={}", rows.size(), merchantId);
        return rows;
    }

    private OverviewStatsResponse.ChartData buildChart(String merchantId, LocalDate today) {
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        List<DailyStat> stats = overviewMapper.dailyStats(merchantId, start);
        Map<LocalDate, DailyStat> byDate = new HashMap<>();
        if (stats != null) {
            for (DailyStat s : stats) {
                byDate.put(s.getD(), s);
            }
        }
        List<String> labels = new ArrayList<>(7);
        List<Long> requests = new ArrayList<>(7);
        List<Long> approved = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            labels.add(d.format(LABEL_FMT));
            DailyStat s = byDate.get(d);
            requests.add(s == null ? 0L : val(s.getTotalCount()));
            approved.add(s == null ? 0L : val(s.getSuccessCount()));
        }
        OverviewStatsResponse.ChartData chart = new OverviewStatsResponse.ChartData();
        chart.setLabels(labels);
        chart.setRequests(requests);
        chart.setApproved(approved);
        return chart;
    }

    private OverviewStatsResponse.ChartData emptyChart() {
        OverviewStatsResponse.ChartData chart = new OverviewStatsResponse.ChartData();
        chart.setLabels(List.of());
        chart.setRequests(List.of());
        chart.setApproved(List.of());
        return chart;
    }

    private double rate(OverviewRangeStat s) {
        long total = val(s.getTotalCount());
        if (total == 0) {
            return 0d;
        }
        return val(s.getSuccessCount()) * 100.0 / total;
    }

    private static double pctChange(long current, long prev) {
        if (prev == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return (current - prev) * 100.0 / prev;
    }

    private static double pctChange(double current, double prev) {
        if (prev == 0d) {
            return current > 0 ? 100.0 : 0.0;
        }
        return (current - prev) * 100.0 / prev;
    }

    private static double pctChangeBd(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.signum() == 0) {
            return current != null && current.signum() > 0 ? 100.0 : 0.0;
        }
        return ((current - prev) / prev).doubleValue() * 100.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static long val(Long v) {
        return v == null ? 0L : v;
    }

    private static OverviewRangeStat defaultIfNull(OverviewRangeStat s) {
        return s == null ? new OverviewRangeStat() : s;
    }
}
