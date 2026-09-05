package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 用户生命周期关键时间点（ISO 8601 格式）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Timestamps {
    /** 基础合规筛查发起时间 */
    private String basicScreeningInitiatedAt;
    /** 用户创建时间 */
    private String userCreatedAt;
    /** 待处理状态时间 */
    private String pendingAt;
    /** 用户通过审批时间 */
    private String approvedAt;
    /** 用户通过审批时间（同 approvedAt，字段名变体） */
    private String userApprovedAt;
    /** 用户被拒绝时间 */
    private String userRejectedAt;
    /** 试用用户时间 */
    private String trialUserAt;
    /** KYC 发起时间 */
    private String kycInitiatedAt;
    /** KYC 待处理时间 */
    private String kycPendingAt;
    /** KYC 通过时间 */
    private String kycSuccessAt;
    /** KYC 失败时间 */
    private String kycFailedAt;
}
