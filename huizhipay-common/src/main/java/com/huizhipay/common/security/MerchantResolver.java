package com.huizhipay.common.security;

/**
 * 商户解析器：从当前登录上下文解析出商户业务键 merchantId。
 * 实现位于 huizhipay-merchant 模块，避免其他领域模块反向依赖 merchant。
 */
public interface MerchantResolver {

    /**
     * 获取当前登录用户的商户访问上下文。
     *
     * @return 已验证的商户与角色；未登录、非商户成员或待接受邀请时为 null
     */
    MerchantAccess getCurrentMerchantAccess();

    /**
     * 获取当前登录用户所属商户的 merchantId。
     *
     * @return merchantId，若当前用户尚未完成入驻则为 null
     */
    default String getCurrentMerchantId() {
        MerchantAccess access = getCurrentMerchantAccess();
        return access == null ? null : access.merchantId();
    }

    /** 服务端从登录身份解析出的商户上下文，不能由请求参数指定。 */
    record MerchantAccess(String merchantId, String role) {}
}
