package com.huizhipay.merchant.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantKybGuard;
import com.huizhipay.merchant.entity.Merchant;
import com.huizhipay.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantKybGuardImpl implements MerchantKybGuard {
    private final MerchantMapper merchantMapper;

    @Override
    public void requireApproved(String merchantId) {
        Merchant merchant = merchantId == null ? null : merchantMapper.selectOne(
                new QueryWrapper<Merchant>().eq("merchant_id", merchantId));
        if (merchant == null || merchant.getKybStatus() != Merchant.KybStatus.APPROVED) {
            throw new BizException(403, "KYB approval required before creating a payment order");
        }
    }
}
