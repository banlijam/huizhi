package com.huizhipay.common.security;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantResolver.MerchantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class MerchantAccessGuard {

    public static final String OWNER = "OWNER";
    public static final String ADMIN = "ADMIN";
    public static final String ANALYST = "ANALYST";
    public static final String READONLY = "READONLY";

    private final MerchantResolver merchantResolver;

    public MerchantAccess requireAnyRole(String... allowedRoles) {
        MerchantAccess access = merchantResolver.getCurrentMerchantAccess();
        if (access == null || access.role() == null
                || Arrays.stream(allowedRoles).noneMatch(access.role()::equalsIgnoreCase)) {
            throw new BizException(403, "Insufficient merchant permission");
        }
        return access;
    }
}
