package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.huizhipay.acquiring.entity.MerchantApiKey;
import com.huizhipay.acquiring.mapper.MerchantApiKeyMapper;
import com.huizhipay.common.exceptions.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class MerchantApiKeyService {
    private static final Set<String> ALLOWED_SCOPES = Set.of("payments:read", "payments:write");
    private final MerchantApiKeyMapper mapper;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public List<KeyView> list(String merchantId) {
        return mapper.selectList(new QueryWrapper<MerchantApiKey>().eq("merchant_id", merchantId)
                .orderByDesc("created_at", "id").last("limit 100")).stream().map(this::view).toList();
    }

    @Transactional
    public IssuedKey issuePending(String merchantId, String actor, IssueCommand command) {
        String env = environment(command == null ? null : command.environment());
        String suppliedName = command == null ? null : command.keyName();
        String name = suppliedName == null || suppliedName.isBlank() ? "Default " + env + " Key" : suppliedName.trim();
        if (name.length() > 64) throw new BizException(400, "Key name is limited to 64 characters");
        String scopes = normalizeScopes(command == null ? null : command.scopes());
        byte[] bytes = new byte[36]; random.nextBytes(bytes);
        String raw = "hzp_test_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        MerchantApiKey key = new MerchantApiKey().setMerchantId(merchantId).setKeyPrefix(raw.substring(0, 20))
                .setKeyHash(hash(raw)).setEnvironment(env).setStatus("PENDING").setEnabled(false)
                .setKeyName(name).setScopes(scopes).setCreatedBy(actor).setCreatedAt(now)
                .setExpiresAt(command == null ? null : command.expiresAt());
        mapper.insert(key); audit(key, "ISSUED_PENDING", actor, "Awaiting explicit activation");
        return new IssuedKey(raw, view(key));
    }

    @Transactional
    public KeyView activate(String merchantId, long keyId, String actor, String secretKey) {
        List<Long> locked = jdbc.query("select id from t_merchant_api_key where merchant_id=? and id=? for update", (rs,n)->rs.getLong(1), merchantId, keyId);
        if (locked.isEmpty()) throw new BizException(404, "Pending API key not found");
        MerchantApiKey pending = mapper.selectById(keyId);
        if (!"PENDING".equals(pending.getStatus())) throw new BizException(409, "API key is not pending");
        if (secretKey == null || !constantEquals(hash(secretKey), pending.getKeyHash())) throw new BizException(403, "The new secret key is required to confirm activation");
        if (pending.getExpiresAt() != null && !pending.getExpiresAt().isAfter(LocalDateTime.now())) throw new BizException(410, "Pending API key has expired");
        LocalDateTime now = LocalDateTime.now();
        List<MerchantApiKey> replaced = mapper.selectList(new QueryWrapper<MerchantApiKey>().eq("merchant_id", merchantId)
                .eq("environment", pending.getEnvironment()).eq("enabled", true));
        mapper.update(null, new UpdateWrapper<MerchantApiKey>().eq("merchant_id", merchantId).eq("environment", pending.getEnvironment())
                .eq("enabled", true).set("enabled", false).set("status", "REVOKED").set("disabled_at", now)
                .set("disabled_by", actor).set("revocation_reason", "rotated"));
        try {
            int changed = mapper.update(null, new UpdateWrapper<MerchantApiKey>().eq("id", keyId).eq("status", "PENDING")
                    .set("enabled", true).set("status", "ACTIVE").set("activated_at", now));
            if (changed != 1) throw new BizException(409, "API key activation conflict");
        } catch (DuplicateKeyException e) { throw new BizException(409, "Another active key already exists for this environment"); }
        pending.setEnabled(true).setStatus("ACTIVE").setActivatedAt(now); audit(pending, "ACTIVATED", actor, "Confirmed rotation");
        replaced.forEach(old -> audit(old, "REVOKED", actor, "rotated"));
        return view(pending);
    }

    @Transactional
    public void revoke(String merchantId, long keyId, String actor, String reason) {
        String why = reason == null || reason.isBlank() ? "revoked by merchant" : reason.trim();
        if (why.length() > 256) throw new BizException(400, "Revocation reason is limited to 256 characters");
        int updated = mapper.update(null, new UpdateWrapper<MerchantApiKey>().eq("id", keyId).eq("merchant_id", merchantId)
                .ne("status", "REVOKED").set("enabled", false).set("status", "REVOKED").set("disabled_at", LocalDateTime.now())
                .set("disabled_by", actor).set("revocation_reason", why));
        if (updated != 1) throw new BizException(404, "API key not found");
        MerchantApiKey key = mapper.selectById(keyId); audit(key, "REVOKED", actor, why);
    }

    private String environment(String value) { String env = value == null ? "TEST" : value.trim().toUpperCase(Locale.ROOT); if (!"TEST".equals(env)) throw new BizException(400,"Only TEST API keys are available"); return env; }
    private String normalizeScopes(Set<String> input) { Set<String> scopes=input==null||input.isEmpty()?ALLOWED_SCOPES:input; if(!ALLOWED_SCOPES.containsAll(scopes)) throw new BizException(400,"Unsupported API key scope"); return scopes.stream().sorted().reduce((a,b)->a+" "+b).orElseThrow(); }
    private void audit(MerchantApiKey k,String action,String actor,String detail){ jdbc.update("insert into t_merchant_api_key_audit(key_id,merchant_id,environment,action,actor,detail) values(?,?,?,?,?,?)",k.getId(),k.getMerchantId(),k.getEnvironment(),action,actor,detail); }
    private String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private boolean constantEquals(String a,String b){return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII),b.getBytes(StandardCharsets.US_ASCII));}
    private KeyView view(MerchantApiKey k){return new KeyView(k.getId(),k.getKeyPrefix(),k.getKeyName(),k.getEnvironment(),k.getStatus(),k.getScopes(),k.getCreatedBy(),k.getDisabledBy(),k.getCreatedAt(),k.getActivatedAt(),k.getDisabledAt(),k.getLastUsedAt(),k.getExpiresAt(),k.getRevocationReason());}
    public record IssueCommand(String environment,String keyName,Set<String> scopes,LocalDateTime expiresAt){}
    public record ActivateCommand(String secretKey){}
    public record RevokeCommand(String reason){}
    public record KeyView(Long id,String keyPrefix,String keyName,String environment,String status,String scopes,String createdBy,String disabledBy,LocalDateTime createdAt,LocalDateTime activatedAt,LocalDateTime disabledAt,LocalDateTime lastUsedAt,LocalDateTime expiresAt,String revocationReason){}
    public record IssuedKey(String secretKey,KeyView key){}
}
