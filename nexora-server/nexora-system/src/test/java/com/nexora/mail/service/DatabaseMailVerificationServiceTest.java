package com.nexora.mail.service;

import com.aurora.starter.verification.config.VerificationProperties;
import com.aurora.starter.verification.redis.RedisMailVerificationRepository;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import com.nexora.config.SysConfigGroupReader;
import com.nexora.domain.form.system.config.EmailConfigForm;
import com.nexora.mail.infrastructure.SmtpMailSenderFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatabaseMailVerificationServiceTest {

    @Test
    void refusesToSendWhenDatabaseEmailConfigurationIsDisabled() {
        SysConfigGroupReader configReader = mock(SysConfigGroupReader.class);
        SmtpMailSenderFactory mailSenderFactory = mock(SmtpMailSenderFactory.class);
        EmailConfigForm config = new EmailConfigForm();
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
}
