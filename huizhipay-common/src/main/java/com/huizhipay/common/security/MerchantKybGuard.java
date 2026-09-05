package com.huizhipay.common.security;

/**
 * Cross-module KYB boundary used by payment entry points.
 * The merchant module owns the database lookup; callers only ask for approval.
 */
public interface MerchantKybGuard {

    /**
     * Requires the merchant's persisted KYB status to be APPROVED.
     *
     * @throws com.huizhipay.common.exceptions.BizException when the merchant is
     *         missing or has not completed KYB
     */
    void requireApproved(String merchantId);
}
