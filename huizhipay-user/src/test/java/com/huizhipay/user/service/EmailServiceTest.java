package com.huizhipay.user.service;

import com.huizhipay.user.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Locale;

/**
 * EmailService 测试类
 * 用于测试邮件发送功能
 * <p>
 * 使用方式：
 * 1. 修改 application-test.yml 中的邮箱配置
 * 2. 在测试方法中设置收件人邮箱
 * 3. 运行对应的测试方法
 */
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
class EmailServiceTest {
    @Autowired
    private EmailService emailService;
    private static final String toEmail = "541493725@qq.com"; // 请替换为测试邮箱
    private static final String token = "test-verification-token-12345";

    /**
     * 测试发送中文邮箱验证邮件
     * 请将 toEmail 替换为你要测试的邮箱地址
     */
    @Test
    void testSendVerificationEmailChinese() throws InterruptedException {
        System.out.println("=== 发送中文邮箱验证邮件 ===");
        System.out.println("收件人: " + toEmail);
        System.out.println("Token: " + token);
        emailService.sendVerificationEmail(toEmail, token);

        // 等待异步发送完成
        Thread.sleep(3000);
        System.out.println("邮件发送请求已提交，请检查收件箱");
    }

    /**
     * 测试发送英文邮箱验证邮件
     * 请将 toEmail 替换为你要测试的邮箱地址
     */
    @Test
    void testSendVerificationEmailEnglish() throws InterruptedException {
        System.out.println("=== 发送英文邮箱验证邮件 ===");
        System.out.println("收件人: " + toEmail);
        System.out.println("Token: " + token);

        emailService.sendVerificationEmail(toEmail, token, Locale.ENGLISH);

        Thread.sleep(3000);
        System.out.println("邮件发送请求已提交，请检查收件箱");
    }

    /**
     * 测试发送中文重置密码邮件
     * 请将 toEmail 替换为你要测试的邮箱地址
     */
    @Test
    void testSendResetPasswordEmailChinese() throws InterruptedException {
        System.out.println("=== 发送中文重置密码邮件 ===");
        System.out.println("收件人: " + toEmail);
        System.out.println("Token: " + token);

        emailService.sendResetPasswordEmail(toEmail, token);

        Thread.sleep(3000);
        System.out.println("邮件发送请求已提交，请检查收件箱");
    }

    /**
     * 测试发送英文重置密码邮件
     * 请将 toEmail 替换为你要测试的邮箱地址
     */
    @Test
    void testSendResetPasswordEmailEnglish() throws InterruptedException {
        System.out.println("=== 发送英文重置密码邮件 ===");
        System.out.println("收件人: " + toEmail);
        System.out.println("Token: " + token);

        emailService.sendResetPasswordEmail(toEmail, token, Locale.ENGLISH);

        Thread.sleep(3000);
        System.out.println("邮件发送请求已提交，请检查收件箱");
    }

    /**
     * 测试发送指定邮箱的验证邮件（可直接修改参数运行）
     */
    @Test
    void testSendToSpecificEmail() throws InterruptedException {
        // 请修改以下参数进行测试
        String recipientEmail = toEmail;  // 填写收件人邮箱
        String verificationToken = "manual-test-" + System.currentTimeMillis();

        if (recipientEmail.isEmpty()) {
            System.out.println("请在 testSendToSpecificEmail 方法中设置 recipientEmail 参数");
            return;
        }

        System.out.println("=== 发送测试邮件 ===");
        System.out.println("收件人: " + recipientEmail);
        System.out.println("Token: " + verificationToken);

        // 发送中文验证邮件
        emailService.sendVerificationEmail(recipientEmail, verificationToken);

        Thread.sleep(3000);
        System.out.println("邮件发送完成！");
    }
}