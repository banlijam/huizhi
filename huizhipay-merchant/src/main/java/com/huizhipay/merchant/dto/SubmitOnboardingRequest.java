package com.huizhipay.merchant.dto;

import com.huizhipay.merchant.entity.Merchant.SettlementPref;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitOnboardingRequest {
    @NotBlank(message = "{merchant.onboarding.field_required}")
    private String company;
    @NotBlank(message = "{merchant.onboarding.field_required}")
    private String country;
    @NotBlank(message = "{merchant.onboarding.field_required}")
    private String licenseNo;
    private String licenseFileUrl;
    @NotBlank(message = "{merchant.onboarding.field_required}")
    private String legalRep;
    @NotBlank(message = "{merchant.onboarding.field_required}")
    private String idNo;
    /** 结算偏好，由前端选择后提交，后端不设默认值 */
    @NotNull(message = "{merchant.onboarding.field_required}")
    private SettlementPref settlementPref;
}
