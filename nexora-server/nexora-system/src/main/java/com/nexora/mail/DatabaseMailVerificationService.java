package com.nexora.mail;

import com.aurora.starter.verification.config.VerificationProperties;
import com.aurora.starter.verification.mail.DefaultMailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.redis.RedisMailVerificationRepository;
import com.aurora.starter.verification.support.VerificationCodeGenerator;
import com.nexora.config.SysConfigGroupReader;
import com.nexora.constants.CommonConstants;
import com.nexora.domain.form.system.config.EmailConfigForm;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseMailVerificationService implements MailVerificationService {

    private static final String UNUSED_FROM_ADDRESS = "unused@example.invalid";

    private final SysConfigGroupReader configReader;
    private final SmtpMailSenderFactory mailSenderFactory;
    private final RedisMailVerificationRepository repository;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationProperties properties;

    @Override
    public void send(MailVerificationSendRequest request) {
        EmailConfigForm config = configReader.email();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new IllegalArgumentException(CommonConstants.SYSTEM_MAIL_DISABLED_MESSAGE);
        }
        delegate(mailSenderFactory.create(config), mailSenderFactory.fromAddress(config)).send(request);
    }

    @Override
    public boolean verifyAndConsume(MailVerificationVerifyRequest request) {
        return delegate(new JavaMailSenderImpl(), UNUSED_FROM_ADDRESS).verifyAndConsume(request);
    }

    private DefaultMailVerificationService delegate(JavaMailSender mailSender, String from) {
        return new DefaultMailVerificationService(mailSender, repository, codeGenerator, properties, from);
    }
}
