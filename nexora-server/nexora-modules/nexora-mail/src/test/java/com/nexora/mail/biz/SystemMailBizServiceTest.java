package com.nexora.mail.biz;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.system.config.SysConfigGroupReader;
import com.nexora.system.domain.form.EmailConfigForm;
import com.nexora.system.domain.form.SystemConfigForm;
import com.nexora.mail.infrastructure.SmtpMailSenderFactory;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemMailBizServiceTest {

    private final SysConfigGroupReader configReader = mock(SysConfigGroupReader.class);
    private final SmtpMailSenderFactory mailSenderFactory = mock(SmtpMailSenderFactory.class);
    private final SystemMailBizService service = new SystemMailBizService(configReader, mailSenderFactory);

    @Test
    void sendsATestMessageWithTheSavedDatabaseConfiguration() {
        EmailConfigForm email = emailConfig(true);
        SystemConfigForm system = new SystemConfigForm();
        system.setSiteName("Nexora Admin");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(configReader.email()).thenReturn(email);
        when(configReader.system()).thenReturn(system);
        when(mailSenderFactory.fromAddress(email)).thenReturn("sender@example.com");

        JavaMailSenderImpl realSender = mock(JavaMailSenderImpl.class);
        when(mailSenderFactory.create(email)).thenReturn(realSender);
        when(realSender.createMimeMessage()).thenReturn(message);

        service.sendTestMail("recipient@example.com");

        verify(realSender).send(message);
    }

    @Test
    void refusesATestMessageWhenEmailIsDisabled() {
        when(configReader.email()).thenReturn(emailConfig(false));

        assertThatThrownBy(() -> service.sendTestMail("recipient@example.com"))
                .isInstanceOf(BizException.class)
                .hasMessage("邮件服务未启用");
        verifyNoInteractions(mailSenderFactory);
    }

    private static EmailConfigForm emailConfig(boolean enabled) {
        EmailConfigForm config = new EmailConfigForm();
        config.setEnabled(enabled);
        config.setHost("smtp.example.com");
        config.setPort(465);
        config.setUsername("sender@example.com");
        config.setPassword("secret");
        config.setFromName("Nexora Admin");
        config.setSsl(true);
        return config;
    }
}
