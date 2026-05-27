package com.huizhipay.user.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String email, String token) {
        sendVerificationEmail(email, token, Locale.US);
    }

    @Async
    public void sendVerificationEmail(String email, String token, Locale locale) {
        String subject = getSubject("verify_email", locale);
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        String content = loadEmailTemplate("verify_email", locale, verifyUrl);
        sendHtmlEmail(email, subject, content);
    }

    @Async
    public void sendResetPasswordEmail(String email, String token) {
        sendResetPasswordEmail(email, token, Locale.US);
    }

    @Async
    public void sendResetPasswordEmail(String email, String token, Locale locale) {
        String subject = getSubject("reset_password", locale);
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String content = loadEmailTemplate("reset_password", locale, resetUrl);
        sendHtmlEmail(email, subject, content);
    }

    /**
     * 根据邮件类型和语言获取主题
     */
    private String getSubject(String templateType, Locale locale) {
        return switch (templateType) {
            case "verify_email" ->
                    locale.getLanguage().equals("en") ? "HuiZhiPay - Email Verification" : "绘智付 - 邮箱激活";
            case "reset_password" ->
                    locale.getLanguage().equals("en") ? "HuiZhiPay - Reset Password" : "绘智付 - 重置密码";
            default -> "绘智付 - 通知";
        };
    }

    /**
     * 加载邮件 HTML 模板并替换动态内容
     *
     * @param templateType 模板类型（verify_email / reset_password）
     * @param locale       语言环境
     * @param url          动态链接
     * @return 渲染后的 HTML 内容
     */
    private String loadEmailTemplate(String templateType, Locale locale, String url) {
        String lang = locale.getLanguage().equals("en") ? "en_US" : "zh_CN";
        String templatePath = "classpath:templates/email/" + templateType + "_" + lang + ".html";

        try {
            Resource resource = resourceLoader.getResource(templatePath);
            if (!resource.exists()) {
                // 回退到中文模板
                templatePath = "classpath:templates/email/" + templateType + "_zh_CN.html";
                resource = resourceLoader.getResource(templatePath);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }

                // 替换模板中的占位符
                String placeholder = templateType.equals("verify_email") ? "{{VERIFY_URL}}" : "{{RESET_URL}}";
                return content.toString().replace(placeholder, url);
            }
        } catch (Exception e) {
            log.error("加载邮件模板失败: {}, 错误: {}", templatePath, e.getMessage(), e);
            // 返回纯文本降级内容
            return getFallbackContent(templateType, locale, url);
        }
    }

    /**
     * 模板加载失败时的降级内容
     */
    private String getFallbackContent(String templateType, Locale locale, String url) {
        if (locale.getLanguage().equals("en")) {
            return switch (templateType) {
                case "verify_email" -> "Thank you for registering! Please click the link to verify your email: " + url;
                case "reset_password" -> "You are requesting to reset your password. Please click the link: " + url;
                default -> "Please click the link: " + url;
            };
        }
        return switch (templateType) {
            case "verify_email" -> "感谢注册！请点击链接激活邮箱: " + url;
            case "reset_password" -> "您正在重置密码，请点击链接: " + url;
            default -> "请点击链接: " + url;
        };
    }

    /**
     * 发送 HTML 格式邮件
     */
    private void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true 表示 HTML 内容
            mailSender.send(message);
            log.info("HTML邮件发送成功，收件人: {}", to);
        } catch (Exception e) {
            log.error("HTML邮件发送失败，收件人: {}, 错误: {}", to, e.getMessage(), e);
        }
    }
}