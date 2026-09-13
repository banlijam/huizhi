package com.huizhipay.acquiring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class WebhookSecretCipher {
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public WebhookSecretCipher(@Value("${huizhipay.webhooks.master-key:}") String encodedKey) {
        byte[] decoded = encodedKey == null || encodedKey.isBlank() ? new byte[0] : Base64.getDecoder().decode(encodedKey);
        this.key = decoded.length == 32 ? decoded : null;
    }

    public boolean configured() { return key != null; }

    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return "v1." + Base64.getUrlEncoder().withoutPadding().encodeToString(iv) + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Unable to encrypt webhook secret", e); }
    }

    public String decrypt(String value) {
        requireKey();
        try {
            String[] parts = value.split("\\.", 3);
            if (parts.length != 3 || !"v1".equals(parts[0])) throw new IllegalArgumentException("Unsupported secret format");
            byte[] iv = Base64.getUrlDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(parts[2])), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Unable to decrypt webhook secret", e); }
    }

    private void requireKey() {
        if (key == null) throw new IllegalStateException("HUIZHIPAY_WEBHOOK_MASTER_KEY must be a base64 encoded 32-byte key");
    }
}
