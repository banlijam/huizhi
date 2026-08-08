package com.huizhipay.merchant.dto;

import com.huizhipay.merchant.entity.Merchant;
import com.huizhipay.merchant.entity.Merchant.KybStatus;
import com.huizhipay.merchant.entity.Merchant.SettlementPref;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OnboardingStatusResponse {
    private KybStatus status;
    private int currentStep;
    private int totalSteps = 4;
    private CompanyInfo company;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    /**
     * 由商户实体构造入驻状态响应。
     */
    public OnboardingStatusResponse(Merchant m) {
        this.status = m.getKybStatus();
        this.currentStep = m.getCurrentStep() == null ? 1 : m.getCurrentStep();
        this.company = new CompanyInfo(m);
        this.submittedAt = m.getSubmittedAt();
        this.reviewedAt = m.getReviewedAt();
    }

    /**
     * 草稿态响应（未入驻商户的默认状态）。
     */
    public static OnboardingStatusResponse draft() {
        OnboardingStatusResponse resp = new OnboardingStatusResponse();
        resp.setStatus(KybStatus.DRAFT);
        resp.setCurrentStep(1);
        resp.setCompany(new CompanyInfo());
        return resp;
    }

    @Data
    @NoArgsConstructor
    public static class CompanyInfo {
        private String name;
        private String country;
        private String licenseNo;
        private String legalRep;
        private String idNo;
        private SettlementPref settlementPref;

        public CompanyInfo(Merchant m) {
            this.name = m.getCompanyName();
            this.country = m.getCountry();
            this.licenseNo = m.getLicenseNo();
            this.legalRep = m.getLegalRep();
            this.idNo = m.getIdNo();
            this.settlementPref = m.getSettlementPref();
        }
    }
}
