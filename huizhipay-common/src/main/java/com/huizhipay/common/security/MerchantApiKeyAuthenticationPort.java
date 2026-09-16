package com.huizhipay.common.security;

import java.util.Set;

public interface MerchantApiKeyAuthenticationPort {
    MerchantApiKeyPrincipal authenticate(String rawKey, String environment, String requiredScope);

    record MerchantApiKeyPrincipal(long keyId, String merchantId, String environment, Set<String> scopes) {}
}
