package com.huizhipay.merchant.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.merchant.entity.Merchant;
import com.huizhipay.merchant.mapper.MerchantMapper;
import com.huizhipay.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 商户解析器实现：从当前登录上下文取 userId，再查 t_merchant.owner_user_id。
 * 位于 merchant 模块，避免 ledger/risk/settlement 反向依赖 merchant。
 */
@Component
@RequiredArgsConstructor
public class MerchantResolverImpl implements MerchantResolver {

    private final MerchantMapper merchantMapper;

    @Override
    public String getCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            Merchant merchant = merchantMapper.selectOne(
                    new QueryWrapper<Merchant>().eq("owner_user_id", up.getId()));
            return merchant == null ? null : merchant.getMerchantId();
        }
        return null;
    }
}
