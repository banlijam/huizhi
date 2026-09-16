package com.huizhipay.user.controller;

import com.huizhipay.common.i18n.I18nUtils;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.user.dto.*;
import com.huizhipay.user.exception.AuthException;
import com.huizhipay.user.security.UserPrincipal;
import com.huizhipay.user.service.AuthService;
import com.huizhipay.user.service.TotpService;
import com.huizhipay.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TotpService totpService;
    private final UserService userService;
    private final MerchantResolver merchantResolver;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public R<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request, LocaleContextHolder.getLocale());
        return R.ok(I18nUtils.get("auth.register.success"));
    }

    @GetMapping("/verify-email")
    public R<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return R.ok(I18nUtils.get("auth.verify_email.success"));
    }

    @PostMapping("/login")
    public R<AuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        if (authResponse.getAccessToken() != null) {
            // 将JWT写入Cookie
            response.addHeader(HttpHeaders.SET_COOKIE, authCookie(authResponse.getAccessToken(), 86400).toString());
            // 不返回token给前端
            authResponse.setAccessToken(null);
        }
        return R.ok(authResponse);
    }

    @GetMapping("/csrf")
    public R<java.util.Map<String, String>> csrf(CsrfToken token) { return R.ok(java.util.Map.of("token", token.getToken())); }

    @PostMapping("/totp/setup")
    @PreAuthorize("isAuthenticated()") // 需要登录后绑定
    public R<TotpSetupResponse> setupTotp(@AuthenticationPrincipal UserPrincipal principal) {
        return R.ok(totpService.generateSecret(principal.getEmail()));
    }

    @PostMapping("/totp/confirm")
    @PreAuthorize("isAuthenticated()")
    public R<Void> confirmTotp(@AuthenticationPrincipal UserPrincipal principal,
                               @RequestBody @Valid TotpConfirmRequest request) {
        boolean valid = totpService.verifyCode(request.getSecret(), request.getCode());
        if (!valid) throw new AuthException(I18nUtils.get("auth.totp.code.error"));
        userService.enableTotp(principal.getId(), request.getSecret());
        return R.ok(I18nUtils.get("auth.totp.setup.success"));
    }

    @PostMapping("/forgot-password")
    public R<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request, LocaleContextHolder.getLocale());
        return R.ok(I18nUtils.get("auth.forgot_password.success"));
    }

    @PostMapping("/reset-password")
    public R<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        return R.ok(I18nUtils.get("auth.reset_password.success"));
    }

    @GetMapping("/me")
    public R<UserInfoResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return R.fail(401, "Unauthorized");
        }
        MerchantResolver.MerchantAccess merchantAccess = merchantResolver.getCurrentMerchantAccess();
        return R.ok(new UserInfoResponse(
                principal.getId(),
                principal.getEmail(),
                null,
                null,
                null,
                null,
                merchantAccess == null ? null : merchantAccess.merchantId(),
                merchantAccess == null ? null : merchantAccess.role()
        ));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletResponse response) {
        // 清除 access_token Cookie
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie("", 0).toString());
        return R.ok(I18nUtils.get("auth.logout.success"));
    }

    private ResponseCookie authCookie(String value, long maxAge) {
        return ResponseCookie.from("access_token", value).httpOnly(true).secure(cookieSecure).path("/")
                .sameSite(cookieSecure ? "Strict" : "Lax").maxAge(maxAge).build();
    }
}
