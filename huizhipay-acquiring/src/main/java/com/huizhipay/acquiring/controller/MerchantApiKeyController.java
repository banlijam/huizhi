package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantApiKeyService;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantKybGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import static com.huizhipay.common.security.MerchantAccessGuard.ADMIN;
import static com.huizhipay.common.security.MerchantAccessGuard.ANALYST;
import static com.huizhipay.common.security.MerchantAccessGuard.OWNER;
import static com.huizhipay.common.security.MerchantAccessGuard.READONLY;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/api/v1/developer/api-keys")
@RequiredArgsConstructor
public class MerchantApiKeyController {
    private final MerchantApiKeyService service;
    private final MerchantAccessGuard accessGuard;
    private final MerchantKybGuard kybGuard;

    @Value("${app.frontend.url:http://127.0.0.1:3000}")
    private String frontendUrl;

    @GetMapping
    public R<List<MerchantApiKeyService.KeyView>> list() {
        String merchantId = accessGuard.requireAnyRole(OWNER, ADMIN, ANALYST, READONLY).merchantId();
        return R.ok(service.list(merchantId));
    }

    @PostMapping
    public R<MerchantApiKeyService.IssuedKey> issue(HttpServletRequest request) {
        requireWrite(request);
        String merchantId = accessGuard.requireAnyRole(OWNER, ADMIN).merchantId();
        kybGuard.requireApproved(merchantId);
        return R.ok(service.issue(merchantId));
    }

    @DeleteMapping("/{keyId}")
    public R<Void> disable(@PathVariable long keyId, HttpServletRequest request) {
        requireWrite(request);
        String merchantId = accessGuard.requireAnyRole(OWNER, ADMIN).merchantId();
        service.disable(merchantId, keyId);
        return R.ok("Test API key disabled");
    }

    private void requireWrite(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String token = request.getHeader("X-HuizhiPay-CSRF");
        try {
            URI expected = URI.create(frontendUrl);
            URI actual = URI.create(origin == null ? "" : origin);
            if (!"1".equals(token) || !sameOrigin(expected, actual)) throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            throw new BizException(403, "CSRF validation failed");
        }
    }

    private boolean sameOrigin(URI a, URI b) {
        return a.getScheme() != null && a.getScheme().equalsIgnoreCase(b.getScheme())
                && a.getHost() != null && a.getHost().equalsIgnoreCase(b.getHost()) && port(a) == port(b);
    }

    private int port(URI uri) {
        if (uri.getPort() != -1) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
