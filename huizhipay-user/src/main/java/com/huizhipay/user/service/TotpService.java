package com.huizhipay.user.service;

import com.huizhipay.user.dto.TotpSetupResponse;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TotpService {
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public TotpSetupResponse generateSecret(String email) {
        log.debug("[Totp] 生成TOTP密钥 email={}", email);
        // 生成密钥（16位Base32）
        String secret = gAuth.createCredentials().getKey();
        // 生成二维码内容 (otpauth://totp/...)
        String qrCodeUrl = "otpauth://totp/HuiZhiPay:" + email + "?secret=" + secret + "&issuer=HuiZhiPay";
        log.debug("[Totp] 密钥生成完成 email={}", email);
        return new TotpSetupResponse(secret, qrCodeUrl);
    }

    public boolean verifyCode(String secret, int code) {
        boolean valid = gAuth.authorize(secret, code);
        log.debug("[Totp] 验证码校验 result={}", valid);
        // 允许前后1个时间窗口偏移（防止手机时间偏差）
        return valid;
    }
}
