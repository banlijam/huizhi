package com.huizhipay.risk.dto;

import com.huizhipay.risk.entity.RiskRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskRuleResponse {
    private String id;
    private boolean enabled;
    private String category;

    public static RiskRuleResponse from(RiskRule r) {
        RiskRuleResponse resp = new RiskRuleResponse();
        resp.id = r.getRuleId() == null ? null : r.getRuleId().name();
        resp.enabled = Boolean.TRUE.equals(r.getEnabled());
        resp.category = r.getCategory() == null ? null : r.getCategory().name();
        return resp;
    }
}
