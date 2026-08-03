package com.nexora.mail.infrastructure;

import com.nexora.system.domain.form.EmailConfigForm;
import jakarta.mail.internet.InternetAddress;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class SmtpMailSenderFactory {

    public JavaMailSenderImpl create(EmailConfigForm config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        if (Boolean.TRUE.equals(config.getSsl())) {
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.ssl.trust", config.getHost());
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.port", String.valueOf(config.getPort()));
        } else {
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        }
        return mailSender;
    }

    public String fromAddress(EmailConfigForm config) {
        if (config.getFromName() == null || config.getFromName().isBlank()) {
            return config.getUsername();
        }
        try {
            return new InternetAddress(config.getUsername(), config.getFromName(),
                    StandardCharsets.UTF_8.name()).toString();
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalArgumentException("发件人名称格式不正确", exception);
        }
    }
}
