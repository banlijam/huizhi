package com.huizhipay.user.service;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceUnitTest {

    @Mock private JavaMailSender mailSender;
    @Mock private ResourceLoader resourceLoader;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, resourceLoader);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://merchant.example");
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@example.com");
        when(mailSender.createMimeMessage())
                .thenAnswer(ignored -> new JavaMailSenderImpl().createMimeMessage());
        when(resourceLoader.getResource(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0, String.class);
            String placeholder = path.contains("reset_password") ? "{{RESET_URL}}" : "{{VERIFY_URL}}";
            return new ByteArrayResource(("<a href=\"" + placeholder + "\">Continue</a>")
                    .getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    void verificationEmailUsesEnglishSubjectAndRequestedRecipient() throws Exception {
        emailService.sendVerificationEmail("buyer@example.com", "verify-token", Locale.ENGLISH);

        MimeMessage message = sentMessage();
        assertThat(message.getSubject()).isEqualTo("HuiZhiPay - Email Verification");
        assertThat(message.getAllRecipients()).extracting(Address::toString)
                .containsExactly("buyer@example.com");
    }

    @Test
    void resetEmailUsesChineseSubjectWithoutExternalSmtp() throws Exception {
        emailService.sendResetPasswordEmail("owner@example.com", "reset-token", Locale.SIMPLIFIED_CHINESE);

        MimeMessage message = sentMessage();
        assertThat(message.getSubject()).isEqualTo("绘智付 - 重置密码");
        assertThat(message.getAllRecipients()).extracting(Address::toString)
                .containsExactly("owner@example.com");
    }

    private MimeMessage sentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
