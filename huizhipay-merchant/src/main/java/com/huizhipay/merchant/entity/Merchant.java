package com.huizhipay.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huizhipay.merchant.dto.SubmitOnboardingRequest;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 商户主体（KYB） */
@Data
@Accessors(chain = true)
@TableName("t_merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务键，如 M-20260808-xxxx */
    private String merchantId;
    private Long ownerUserId;
    private String companyName;
    private String country;
    private String licenseNo;
    private String licenseFileUrl;
    private String legalRep;
    private String idNo;
    /** 结算偏好 */
    private SettlementPref settlementPref;
    /** KYB 审核状态 */
    private KybStatus kybStatus;
    private Short currentStep;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 批量应用入驻表单提交字段 */
    public Merchant applyKybFields(SubmitOnboardingRequest req) {
        this.companyName = req.getCompany();
        this.country = req.getCountry();
        this.licenseNo = req.getLicenseNo();
        this.licenseFileUrl = req.getLicenseFileUrl();
        this.legalRep = req.getLegalRep();
        this.idNo = req.getIdNo();
        this.settlementPref = req.getSettlementPref();
        return this;
    }

    /** KYB 审核状态：草稿 / 审核中 / 已通过 / 已拒绝 */
    public enum KybStatus { DRAFT, PENDING, APPROVED, REJECTED }

    /** 结算偏好：加密货币 / 法币 */
    public enum SettlementPref { CRYPTO, FIAT }
}
