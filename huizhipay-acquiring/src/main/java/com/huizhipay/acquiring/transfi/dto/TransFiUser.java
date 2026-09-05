package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * TransFi 用户（同时兼容个人用户和企业用户）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransFiUser {

    // ---------- 基础身份 ----------
    /** 用户唯一标识，如 UX-260224104251483547 */
    private String userId;
    /** 名（个人用户）/ 企业名（企业用户，旧数据可能放在 name 里） */
    private String firstName;
    /** 姓（个人用户） */
    private String lastName;
    /** 兼容旧用户数据的通用名称字段 */
    private String name;
    /** 用户类型：individual / business */
    private String type;
    /** 邮箱 */
    private String email;
    /** 完整手机号（含区号已拼入的场景） */
    private String phone;
    /** 国际区号，如 +91、+1 */
    private String phoneCode;
    /** 国家代码，如 IN、US */
    private String country;
    /** 性别：male / female（个人用户） */
    private String gender;
    /** 出生日期（个人用户），格式 yyyy-MM-dd */
    private String dob;

    // ---------- 地址 ----------
    private Address address;

    // ---------- 状态与 KYC / KYB ----------
    /** 用户整体状态：user_approved / user_rejected / user_pending 等 */
    private String status;

    // KYC（个人用户）与 KYB（企业用户）拼写不同，用 JsonAlias 互相兼容
    @JsonAlias({"basicKycStatus", "basicKybStatus"})
    private String basicKycStatus;

    @JsonAlias({"standardKycStatus", "standardKybStatus"})
    private String standardKycStatus;

    // 兼容 advancedKycStatus、拼写错误的 advanceKycStatus，以及企业用户的 advancedKybStatus
    @JsonAlias({"advancedKycStatus", "advanceKycStatus", "advancedKybStatus"})
    private String advancedKycStatus;

    /** 企业用户独有：简化 KYB 状态 */
    private String simplifiedKybStatus;

    // ---------- 企业用户独有 ----------
    /** 企业名称 */
    private String businessName;
    /** 企业注册号 */
    private String regNo;

    // ---------- 时间戳 ----------
    private Timestamps timestamps;

    // ---------- 邮箱 / 手机验证 ----------
    private Verification emailVerification;
    private Verification phoneVerification;

    // ---------- 扩展信息（旧用户特有） ----------
    private String grpOrgId;
    private Vendors vendors;
    private Customer customer;
    private MetaData metaData;
    /** 旧用户状态字段（与 status 并存） */
    private String state;
    /** OTP 发送次数 */
    private Integer otpCount;
    /** Webhook 失败重试次数 */
    private Integer webhookRetryCount;
    /** 最近一次 OTP 发送渠道 */
    private String lastOtpChannel;

    // ---------- 身份证件（KYC 完成后返回） ----------
    private String idDocType;
    private String idDocNumber;
    private String idDocUserName;
    private String idDocIssuerCountry;
    private String idDocExpiryDate;
    private String nationality;
    /** KYC/KYB 失败时的失败原因 */
    private String failureMessage;
}
