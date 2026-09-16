package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.acquiring.entity.MerchantApiKey;
import com.huizhipay.acquiring.mapper.MerchantApiKeyMapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.security.MerchantApiKeyAuthenticationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantApiKeyAuthenticator implements MerchantApiKeyAuthenticationPort {
    private final MerchantApiKeyMapper mapper;

    @Override
    public MerchantApiKeyPrincipal authenticate(String rawKey, String environment, String requiredScope) {
        if (!"TEST".equals(environment)) throw new BizException(401, "Only TEST API keys are available");
        String expectedPrefix = "hzp_test_";
        if (rawKey == null || !rawKey.matches("^" + expectedPrefix + "[A-Za-z0-9_-]{40,}$")) {
            throw new BizException(401, "Valid " + environment + " API key required");
        }
        byte[] suppliedHash = sha256(rawKey);
        String hashHex = HexFormat.of().formatHex(suppliedHash);
        MerchantApiKey key = mapper.selectOne(Wrappers.<MerchantApiKey>lambdaQuery()
                .eq(MerchantApiKey::getKeyHash, hashHex)
                .eq(MerchantApiKey::getEnvironment, environment)
                .eq(MerchantApiKey::getEnabled, true));
        if (key == null || !"ACTIVE".equals(key.getStatus()) || key.getExpiresAt() != null && !key.getExpiresAt().isAfter(LocalDateTime.now())
                || !MessageDigest.isEqual(suppliedHash, HexFormat.of().parseHex(key.getKeyHash()))) {
            throw new BizException(401, "Valid " + environment + " API key required");
        }
        Set<String> scopes = Arrays.stream(key.getScopes().split("\\s+")).filter(s -> !s.isBlank()).collect(Collectors.toUnmodifiableSet());
        if (!scopes.contains(requiredScope)) throw new BizException(403, "API key scope " + requiredScope + " required");
        mapper.updateById(new MerchantApiKey().setId(key.getId()).setLastUsedAt(LocalDateTime.now()));
        return new MerchantApiKeyPrincipal(key.getId(), key.getMerchantId(), environment, scopes);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
