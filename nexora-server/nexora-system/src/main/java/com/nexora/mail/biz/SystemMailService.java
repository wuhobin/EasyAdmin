package com.nexora.mail.biz;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.config.SysConfigGroupReader;
import com.nexora.constants.CommonConstants;
import com.nexora.domain.form.system.config.EmailConfigForm;
import com.nexora.mail.infrastructure.SmtpMailSenderFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SystemMailService {

    private static final DateTimeFormatter SEND_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysConfigGroupReader configReader;
    private final SmtpMailSenderFactory mailSenderFactory;

    public void sendTestMail(String to) {
        EmailConfigForm config = configReader.email();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new BizException(CommonConstants.SYSTEM_MAIL_DISABLED_MESSAGE);
        }

        try {
            JavaMailSender mailSender = mailSenderFactory.create(config);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(mailSenderFactory.fromAddress(config));
            helper.setTo(to);
            helper.setSubject("【" + configReader.system().getSiteName() + "】测试邮件");
            helper.setText(testContent(config), false);
            mailSender.send(message);
        } catch (MessagingException | MailException | IllegalArgumentException exception) {
            throw new BizException(CommonConstants.TEST_EMAIL_SEND_FAILED_MESSAGE.formatted(exception.getMessage()));
        }
    }

    private static String testContent(EmailConfigForm config) {
        return "这是一封测试邮件，如果您收到此邮件，说明系统邮箱配置正确。\n\n"
                + "发送时间：" + LocalDateTime.now().format(SEND_TIME_FORMAT) + "\n"
                + "SMTP服务器：" + config.getHost() + "\n"
                + "发件人：" + config.getUsername();
    }
}
