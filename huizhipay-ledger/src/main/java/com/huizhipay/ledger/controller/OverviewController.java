package com.huizhipay.ledger.controller;

import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.ledger.dto.LedgerRowResponse;
import com.huizhipay.ledger.dto.OverviewStatsResponse;
import com.huizhipay.ledger.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 指挥中心：今日大盘 + 透明分账账本。
 */
@RestController
@RequestMapping("/api/v1/overview")
@RequiredArgsConstructor
public class OverviewController {

    private final MerchantResolver merchantResolver;
    private final OverviewService overviewService;

    @GetMapping("/stats")
    public R<OverviewStatsResponse> stats() {
        return R.ok(overviewService.getStats(merchantResolver.getCurrentMerchantId()));
    }

    @GetMapping("/ledger")
    public R<List<LedgerRowResponse>> ledger() {
        return R.ok(overviewService.getLedger(merchantResolver.getCurrentMerchantId()));
    }
}
