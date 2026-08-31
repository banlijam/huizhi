package com.huizhipay.risk.controller;

import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.risk.dto.RiskRuleResponse;
import com.huizhipay.risk.dto.ToggleRuleRequest;
import com.huizhipay.risk.service.RiskRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.huizhipay.common.security.MerchantAccessGuard.ADMIN;
import static com.huizhipay.common.security.MerchantAccessGuard.ANALYST;
import static com.huizhipay.common.security.MerchantAccessGuard.OWNER;

/**
 * 风控与智能路由规则：Strict Anti-Fraud Mode + 常规规则开关。
 */
@RestController
@RequestMapping("/api/v1/risk/rules")
@RequiredArgsConstructor
public class RiskRuleController {

    private final MerchantResolver merchantResolver;
    private final MerchantAccessGuard merchantAccessGuard;
    private final RiskRuleService riskRuleService;

    @GetMapping
    public R<List<RiskRuleResponse>> list() {
        return R.ok(riskRuleService.listRules(merchantResolver.getCurrentMerchantId()));
    }

    @PutMapping("/{id}")
    public R<RiskRuleResponse> toggle(@PathVariable("id") String ruleId,
                                      @RequestBody ToggleRuleRequest req) {
        return R.ok(riskRuleService.toggleRule(
                merchantAccessGuard.requireAnyRole(OWNER, ADMIN, ANALYST).merchantId(),
                ruleId, req.isEnabled()));
    }
}
