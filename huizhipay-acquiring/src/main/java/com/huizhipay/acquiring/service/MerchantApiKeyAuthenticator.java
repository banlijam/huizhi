package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.acquiring.entity.MerchantApiKey;
import com.huizhipay.acquiring.mapper.MerchantApiKeyMapper;
import com.huizhipay.common.exceptions.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class MerchantApiKeyAuthenticator {
    private final MerchantApiKeyMapper mapper;

    public String requireMerchant(String rawKey) {
        if (rawKey == null || !rawKey.matches("^hzp_test_[A-Za-z0-9_-]{40,}$")) {
            throw new BizException(401, "Valid Sandbox API key required");
        }
        byte[] suppliedHash = sha256(rawKey);
        String hashHex = HexFormat.of().formatHex(suppliedHash);
        MerchantApiKey key = mapper.selectOne(Wrappers.<MerchantApiKey>lambdaQuery()
                .eq(MerchantApiKey::getKeyHash, hashHex)
                .eq(MerchantApiKey::getEnabled, true));
        if (key == null || !MessageDigest.isEqual(suppliedHash, HexFormat.of().parseHex(key.getKeyHash()))) {
            throw new BizException(401, "Valid Sandbox API key required");
        }
        return key.getMerchantId();
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
