package com.huizhipay.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.i18n.I18nUtils;
import com.huizhipay.risk.dto.RiskRuleResponse;
import com.huizhipay.risk.entity.RiskRule;
import com.huizhipay.risk.entity.RiskRule.RiskCategory;
import com.huizhipay.risk.entity.RiskRule.RiskRuleId;
import com.huizhipay.risk.mapper.RiskRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 风控与智能路由规则服务，每个商户首次访问时懒加载默认规则集 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskRuleService {

    /** 默认规则集（顺序即展示顺序） */
    private static final Map<RiskRuleId, DefaultRule> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(RiskRuleId.STRICT_MODE, new DefaultRule(false, RiskCategory.MASTER));
        DEFAULTS.put(RiskRuleId.BLOCK_PREPAID, new DefaultRule(true, RiskCategory.NORMAL));
        DEFAULTS.put(RiskRuleId.FORCE_US_3DS, new DefaultRule(true, RiskCategory.NORMAL));
        DEFAULTS.put(RiskRuleId.KYT_SCREENING, new DefaultRule(true, RiskCategory.NORMAL));
        DEFAULTS.put(RiskRuleId.BLOCK_HIGH_RISK_REGION, new DefaultRule(false, RiskCategory.NORMAL));
    }

    private final RiskRuleMapper riskRuleMapper;

    public List<RiskRuleResponse> listRules(String merchantId) {
        log.info("[RiskRule] 拉取风控规则 merchantId={}", merchantId);
        if (merchantId == null) {
            log.debug("[RiskRule] 商户未入驻，返回默认模板");
            return defaultTemplate();
        }
        List<RiskRule> rules = riskRuleMapper.selectList(
                new QueryWrapper<RiskRule>().eq("merchant_id", merchantId));
        if (rules.isEmpty()) {
            log.info("[RiskRule] 首次访问，懒加载默认规则集 merchantId={}", merchantId);
            rules = seedDefaults(merchantId);
        }
        List<RiskRuleResponse> result = new ArrayList<>(rules.size());
        for (RiskRuleId ruleId : DEFAULTS.keySet()) {
            rules.stream()
                    .filter(r -> ruleId == r.getRuleId())
                    .findFirst()
                    .ifPresent(r -> result.add(RiskRuleResponse.from(r)));
        }
        log.debug("[RiskRule] 返回{}条规则 merchantId={}", result.size(), merchantId);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public RiskRuleResponse toggleRule(String merchantId, String ruleIdStr, boolean enabled) {
        RiskRuleId ruleId = RiskRuleId.valueOf(ruleIdStr);
        log.info("[RiskRule] 切换风控规则 merchantId={}, ruleId={}, enabled={}", merchantId, ruleId, enabled);
        if (merchantId == null) {
            log.warn("[RiskRule] 商户未入驻，禁止切换规则 ruleId={}", ruleId);
            throw new BizException(403, I18nUtils.get("risk.merchant.not_ready"));
        }
        DefaultRule def = DEFAULTS.get(ruleId);
        if (def == null) {
            log.warn("[RiskRule] 规则不存在 ruleId={}", ruleId);
            throw new BizException(404, I18nUtils.get("risk.rule.not_found"));
        }
        RiskRule rule = riskRuleMapper.selectOne(
                new QueryWrapper<RiskRule>()
                        .eq("merchant_id", merchantId)
                        .eq("rule_id", ruleId.name()));
        if (rule == null) {
            rule = new RiskRule()
                    .setMerchantId(merchantId)
                    .setRuleId(ruleId)
                    .setEnabled(enabled)
                    .setCategory(def.category);
            riskRuleMapper.insert(rule);
            log.info("[RiskRule] 新规则插入 merchantId={}, ruleId={}, enabled={}", merchantId, ruleId, enabled);
        } else {
            rule.setEnabled(enabled);
            riskRuleMapper.updateById(rule);
            log.info("[RiskRule] 规则更新 merchantId={}, ruleId={}, enabled={}", merchantId, ruleId, enabled);
        }
        return RiskRuleResponse.from(rule);
    }

    private List<RiskRule> seedDefaults(String merchantId) {
        List<RiskRule> seeded = new ArrayList<>(DEFAULTS.size());
        for (Map.Entry<RiskRuleId, DefaultRule> e : DEFAULTS.entrySet()) {
            RiskRule r = new RiskRule()
                    .setMerchantId(merchantId)
                    .setRuleId(e.getKey())
                    .setEnabled(e.getValue().enabled)
                    .setCategory(e.getValue().category);
            riskRuleMapper.insert(r);
            seeded.add(r);
        }
        log.debug("[RiskRule] 种子规则写入完成 merchantId={}, 共{}条", merchantId, seeded.size());
        return seeded;
    }

    private List<RiskRuleResponse> defaultTemplate() {
        List<RiskRuleResponse> list = new ArrayList<>(DEFAULTS.size());
        DEFAULTS.forEach((ruleId, def) -> list.add(new RiskRuleResponse(ruleId.name(), def.enabled, def.category.name())));
        return list;
    }

    private record DefaultRule(boolean enabled, RiskCategory category) {
    }
}
