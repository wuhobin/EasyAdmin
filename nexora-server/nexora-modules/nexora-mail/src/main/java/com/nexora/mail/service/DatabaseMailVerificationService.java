package com.nexora.mail.service;

import com.aurora.starter.verification.config.VerificationProperties;
import com.aurora.starter.verification.mail.DefaultMailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.redis.RedisMailVerificationRepository;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import com.nexora.mail.constants.MailConstants;
import com.nexora.system.api.EmailSettings;
import com.nexora.system.api.SystemConfigReader;
import com.nexora.mail.infrastructure.SmtpMailSenderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseMailVerificationService implements MailVerificationService {

    private static final String UNUSED_FROM_ADDRESS = "unused@example.invalid";

    private final SystemConfigReader configReader;
    private final SmtpMailSenderFactory mailSenderFactory;
    private final RedisMailVerificationRepository repository;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationProperties properties;

    @Override
    public void send(MailVerificationSendRequest request) {
        EmailSettings config = configReader.email();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new IllegalArgumentException(MailConstants.SYSTEM_MAIL_DISABLED_MESSAGE);
        }
        delegate(mailSenderFactory.create(config), config.getUsername(), scopedProperties(config)).send(request);
    }

    @Override
    public boolean verifyAndConsume(MailVerificationVerifyRequest request) {
        return delegate(new JavaMailSenderImpl(), UNUSED_FROM_ADDRESS, properties).verifyAndConsume(request);
    }

    private DefaultMailVerificationService delegate(JavaMailSender mailSender, String from,
                                                     VerificationProperties delegateProperties) {
        return new DefaultMailVerificationService(
                mailSender, repository, codeGenerator, delegateProperties, from);
    }

    private VerificationProperties scopedProperties(EmailSettings config) {
        VerificationProperties scoped = new VerificationProperties();
        scoped.setKeyPrefix(properties.getKeyPrefix());
        scoped.setImage(properties.getImage());
        scoped.setSms(properties.getSms());

        VerificationProperties.MailProperties source = properties.getMail();
        VerificationProperties.MailProperties mail = scoped.getMail();
        mail.setEnabled(source.isEnabled());
        mail.setFrom(source.getFrom());
        mail.setFromName(config.getFromName());
        mail.setCodeLength(source.getCodeLength());
        mail.setExpireTime(source.getExpireTime());
        mail.setCooldown(source.getCooldown());
        return scoped;
    }
}
