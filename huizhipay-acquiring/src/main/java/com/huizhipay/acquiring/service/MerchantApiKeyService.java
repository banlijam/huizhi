package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.huizhipay.acquiring.entity.MerchantApiKey;
import com.huizhipay.acquiring.mapper.MerchantApiKeyMapper;
import com.huizhipay.common.exceptions.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantApiKeyService {
    private static final String TEST_PREFIX = "hzp_test_";
    private static final int RANDOM_BYTES = 36;
    private static final int DISPLAY_PREFIX_LENGTH = 20;

    private final MerchantApiKeyMapper mapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public List<KeyView> list(String merchantId) {
        return mapper.selectList(new QueryWrapper<MerchantApiKey>()
                        .eq("merchant_id", merchantId)
                        .orderByDesc("created_at", "id")
                        .last("limit 20"))
                .stream().map(this::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public IssuedKey issue(String merchantId) {
        LocalDateTime now = LocalDateTime.now();
        mapper.update(null, new UpdateWrapper<MerchantApiKey>()
                .eq("merchant_id", merchantId)
                .eq("enabled", true)
                .set("enabled", false)
                .set("disabled_at", now));

        byte[] random = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(random);
        String rawKey = TEST_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        MerchantApiKey key = new MerchantApiKey()
                .setMerchantId(merchantId)
                .setKeyPrefix(rawKey.substring(0, DISPLAY_PREFIX_LENGTH))
                .setKeyHash(sha256Hex(rawKey))
                .setEnabled(true)
                .setCreatedAt(now);
        mapper.insert(key);
        return new IssuedKey(rawKey, toView(key));
    }

    public void disable(String merchantId, long keyId) {
        int updated = mapper.update(null, new UpdateWrapper<MerchantApiKey>()
                .eq("id", keyId)
                .eq("merchant_id", merchantId)
                .eq("enabled", true)
                .set("enabled", false)
                .set("disabled_at", LocalDateTime.now()));
        if (updated != 1) throw new BizException(404, "Active Test API key not found");
    }

    private String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private KeyView toView(MerchantApiKey key) {
        return new KeyView(key.getId(), key.getKeyPrefix(), Boolean.TRUE.equals(key.getEnabled()),
                key.getCreatedAt(), key.getDisabledAt());
    }

    public record KeyView(Long id, String keyPrefix, boolean enabled, LocalDateTime createdAt,
                          LocalDateTime disabledAt) {}
    public record IssuedKey(String secretKey, KeyView key) {}
}
