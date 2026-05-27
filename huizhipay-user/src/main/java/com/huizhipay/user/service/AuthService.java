package com.huizhipay.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.user.config.JwtTokenProvider;
import com.huizhipay.user.dto.*;
import com.huizhipay.user.entity.EmailVerificationToken;
import com.huizhipay.user.entity.EmailVerificationToken.TokenType;
import com.huizhipay.user.entity.User;
import com.huizhipay.common.i18n.I18nUtils;
import com.huizhipay.user.exception.AuthException;
import com.huizhipay.user.mapper.EmailVerificationTokenMapper;
import com.huizhipay.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder; // BCrypt
    private final JwtTokenProvider jwtProvider;
    private final TotpService totpService;
    private final EmailVerificationTokenMapper tokenMapper;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, request.getEmail()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException(I18nUtils.get("auth.login.error"));
        }
        if (!user.getEmailVerified()) {
            throw new AuthException(I18nUtils.get("auth.email.unverified"));
        }
        if (user.getStatus() == 0) {
            throw new AuthException(I18nUtils.get("auth.account.disabled"));
        }

        // 1. 如果开启了TOTP但请求未带验证码 -> 返回标志要求前端二次提交
        if (user.getTotpEnabled() && request.getTotpCode() == null) {
            return new AuthResponse().setTotpRequired(true);
        }

        // 2. 如果开启了TOTP且验证码不为空 -> 校验
        if (user.getTotpEnabled()) {
            boolean valid = totpService.verifyCode(user.getTotpSecret(), request.getTotpCode());
            if (!valid) {
                throw new AuthException(I18nUtils.get("auth.totp.code.invalid"));
            }
        }

        // 3. 生成JWT
        String token = jwtProvider.createToken(Map.of(
                "userId", user.getId()
                , "email", user.getEmail()
        ), user.getEmail());
        return new AuthResponse().setAccessToken(token);
    }

    // 注册
    public void register(RegisterRequest request) throws AuthException {
        register(request, Locale.US);
    }

    public void register(RegisterRequest request, Locale locale) throws AuthException {
        // 检查邮箱是否已被注册（包括未验证的）
        if (userMapper.exists(Wrappers.<User>lambdaQuery().eq(User::getEmail, request.getEmail()))) {
            throw new AuthException(I18nUtils.get("auth.register.email_exists"));
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        userMapper.insert(user);

        // 生成验证令牌（UUID）
        String token = UUID.randomUUID().toString();
        tokenMapper.insert(new EmailVerificationToken()
                .setUserId(user.getId())
                .setToken(token)
                .setType(TokenType.REGISTER)
                .setExpiryDate(LocalDateTime.now().plusMinutes(15)));

        // 异步发送邮件（含激活链接: https://xxx/api/v1/auth/verify-email?token=xxx）
        emailService.sendVerificationEmail(request.getEmail(), token, locale);
    }

    // 验证邮箱
    public void verifyEmail(String token) {
        EmailVerificationToken evt = tokenMapper.selectOne(Wrappers.<EmailVerificationToken>lambdaQuery()
                                                                   .eq(EmailVerificationToken::getToken, token));
        if (evt == null || evt.getUsed() || evt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AuthException(I18nUtils.get("auth.verify.invalid_link"));
        }
        // 标记用户为已验证
        userMapper.updateById(new User().setId(evt.getUserId()).setEmailVerified(true));
        evt.setUsed(true);
        tokenMapper.updateById(evt);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        forgotPassword(request, Locale.US);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, Locale locale) {
        String email = request.getEmail();

        // 1. 查找用户（无论是否存在，统一返回成功，防邮箱枚举）
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .eq(User::getEmailVerified, true)   // 必须已验证邮箱
                        .eq(User::getStatus, 1)             // 必须启用
        );

        // 若用户不存在或未激活/禁用，仍然返回成功，但不发送邮件（或静默处理）
        if (user == null) {
            log.warn("重置密码请求，用户不存在或未激活: {}", email);
            // 直接返回，不发送任何邮件，避免暴露用户存在性
            return;
        }

        // 2. 生成重置令牌
        String token = UUID.randomUUID().toString();
        EmailVerificationToken evt = new EmailVerificationToken();
        evt.setUserId(user.getId());
        evt.setToken(token);
        evt.setType(EmailVerificationToken.TokenType.RESET_PASSWORD);
        evt.setExpiryDate(LocalDateTime.now().plusMinutes(15)); // 15分钟过期
        evt.setUsed(false);
        tokenMapper.insert(evt);

        // 3. 异步发送重置邮件
        emailService.sendResetPasswordEmail(email, token, locale);
        log.info("重置密码邮件已发送至: {}", email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        // 1. 查询令牌（必须为 RESET_PASSWORD 类型）
        EmailVerificationToken evt = tokenMapper.selectOne(
                new LambdaQueryWrapper<EmailVerificationToken>()
                        .eq(EmailVerificationToken::getToken, token)
                        .eq(EmailVerificationToken::getType, EmailVerificationToken.TokenType.RESET_PASSWORD)
        );

        if (evt == null) {
            throw new AuthException(I18nUtils.get("auth.reset.invalid_link"));
        }

        // 2. 校验是否已使用或过期
        if (evt.getUsed()) {
            throw new AuthException(I18nUtils.get("auth.reset.link_used"));
        }
        if (evt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AuthException(I18nUtils.get("auth.reset.link_expired"));
        }

        // 3. 查询用户并校验状态
        User user = userMapper.selectById(evt.getUserId());
        if (user == null || !user.getEmailVerified() || user.getStatus() != 1) {
            // 若用户被禁用或删除，标记令牌已使用并抛出异常
            evt.setUsed(true);
            tokenMapper.updateById(evt);
            throw new AuthException(I18nUtils.get("auth.reset.account_status_error"));
        }

        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        // 5. 标记令牌已使用
        evt.setUsed(true);
        tokenMapper.updateById(evt);

        log.info("用户密码重置成功, userId={}", user.getId());
    }
}