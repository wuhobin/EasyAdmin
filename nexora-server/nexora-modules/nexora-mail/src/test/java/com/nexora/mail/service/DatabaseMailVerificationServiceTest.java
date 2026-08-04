package com.nexora.mail.service;

import com.aurora.starter.verification.config.VerificationProperties;
import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.redis.RedisMailVerificationRepository;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import com.nexora.mail.infrastructure.SmtpMailSenderFactory;
import com.nexora.system.api.EmailSettings;
import com.nexora.system.api.SystemConfigReader;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatabaseMailVerificationServiceTest {

    @Test
    void refusesToSendWhenDatabaseEmailConfigurationIsDisabled() {
        SystemConfigReader configReader = mock(SystemConfigReader.class);
        SmtpMailSenderFactory mailSenderFactory = mock(SmtpMailSenderFactory.class);
        EmailSettings config = new EmailSettings();
        config.setEnabled(false);
        when(configReader.email()).thenReturn(config);
        DatabaseMailVerificationService service = new DatabaseMailVerificationService(
                configReader, mailSenderFactory, mock(RedisMailVerificationRepository.class),
                mock(VerificationCodeGenerator.class), new VerificationProperties());

        assertThatThrownBy(() -> service.send(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("邮件服务未启用");
        verifyNoInteractions(mailSenderFactory);
    }

    @Test
    void sendsWithBareAddressAndDatabaseDisplayName() throws Exception {
        SystemConfigReader configReader = mock(SystemConfigReader.class);
        SmtpMailSenderFactory mailSenderFactory = mock(SmtpMailSenderFactory.class);
        RedisMailVerificationRepository repository = mock(RedisMailVerificationRepository.class);
        VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
        VerificationProperties properties = new VerificationProperties();
        EmailSettings config = enabledConfig();
        JavaMailSenderImpl mailSender = mock(JavaMailSenderImpl.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        when(configReader.email()).thenReturn(config);
        when(mailSenderFactory.create(config)).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(repository.acquireCooldown(anyString(), anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(repository.storeCodeIfCooldownOwned(
                anyString(), anyString(), anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(codeGenerator.generate(anyInt())).thenReturn("123456");

        DatabaseMailVerificationService service = new DatabaseMailVerificationService(
                configReader, mailSenderFactory, repository, codeGenerator, properties);
        service.send(new MailVerificationSendRequest(
                "recipient@example.com",
                CommonVerificationScene.CHANGE_EMAIL,
                "更换邮箱验证码",
                "验证码：{code}",
                MailContentType.TEXT));

        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("sender@example.com");
        assertThat(from.getPersonal()).isEqualTo("Nexora Admin");
        assertThat(properties.getMail().getFromName()).isNull();
    }

    private static EmailSettings enabledConfig() {
        EmailSettings config = new EmailSettings();
        config.setEnabled(true);
        config.setUsername("sender@example.com");
        config.setFromName("Nexora Admin");
        return config;
    }
}
