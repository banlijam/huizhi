package com.huizhipay.common.security;

/**
 * 商户解析器：从当前登录上下文解析出商户业务键 merchantId。
 * 实现位于 huizhipay-merchant 模块，避免其他领域模块反向依赖 merchant。
 */
public interface MerchantResolver {

    /**
     * 获取当前登录用户所属商户的 merchantId。
     *
     * @return merchantId，若当前用户尚未完成入驻则为 null
     */
    String getCurrentMerchantId();
}
