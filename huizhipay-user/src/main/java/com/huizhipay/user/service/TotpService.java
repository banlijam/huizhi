package com.huizhipay.user.service;

import com.huizhipay.user.dto.TotpSetupResponse;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.springframework.stereotype.Service;

@Service
public class TotpService {
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public TotpSetupResponse generateSecret(String email) {
        // 生成密钥（16位Base32）
        String secret = gAuth.createCredentials().getKey();
        // 生成二维码内容 (otpauth://totp/...)
        String qrCodeUrl = "otpauth://totp/HuiZhiPay:" + email + "?secret=" + secret + "&issuer=HuiZhiPay";
        return new TotpSetupResponse(secret, qrCodeUrl);
    }

    public boolean verifyCode(String secret, int code) {
        // 允许前后1个时间窗口偏移（防止手机时间偏差）
        return gAuth.authorize(secret, code);
    }
}