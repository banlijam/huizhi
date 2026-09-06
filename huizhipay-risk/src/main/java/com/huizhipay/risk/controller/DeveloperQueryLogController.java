package com.huizhipay.risk.controller;

import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.risk.dto.DeveloperQueryLogResponse;
import com.huizhipay.risk.service.DeveloperQueryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.huizhipay.common.security.MerchantAccessGuard.ADMIN;
import static com.huizhipay.common.security.MerchantAccessGuard.ANALYST;
import static com.huizhipay.common.security.MerchantAccessGuard.OWNER;
import static com.huizhipay.common.security.MerchantAccessGuard.READONLY;

@RestController
@RequestMapping("/api/v1/developer/query-logs")
@RequiredArgsConstructor
public class DeveloperQueryLogController {

    private final MerchantAccessGuard merchantAccessGuard;
    private final DeveloperQueryLogService developerQueryLogService;

    @GetMapping
    public R<List<DeveloperQueryLogResponse>> list() {
        String merchantId = merchantAccessGuard
                .requireAnyRole(OWNER, ADMIN, ANALYST, READONLY)
                .merchantId();
        return R.ok(developerQueryLogService.latestForMerchant(merchantId));
    }
}
