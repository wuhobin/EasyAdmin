package com.nexora.mail.infrastructure;

import com.nexora.system.api.EmailSettings;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpMailSenderFactoryTest {

    private final SmtpMailSenderFactory factory = new SmtpMailSenderFactory();

    @Test
    void createsAnImplicitSslSenderFromTheDatabaseConfiguration() {
        EmailSettings config = config(true);

        JavaMailSenderImpl sender = factory.create(config);

        assertThat(sender.getHost()).isEqualTo("smtp.example.com");
        assertThat(sender.getPort()).isEqualTo(465);
        assertThat(sender.getUsername()).isEqualTo("sender@example.com");
        assertThat(sender.getPassword()).isEqualTo("secret");
        assertThat(sender.getJavaMailProperties())
                .containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.ssl.enable", "true")
                .doesNotContainKey("mail.smtp.starttls.enable");
        assertThat(factory.fromAddress(config))
                .contains("sender@example.com")
                .doesNotContain("secret");
    }

    @Test
    void usesStartTlsWhenSslIsDisabled() {
        JavaMailSenderImpl sender = factory.create(config(false));

        assertThat(sender.getJavaMailProperties())
                .containsEntry("mail.smtp.starttls.enable", "true")
                .containsEntry("mail.smtp.starttls.required", "true")
                .doesNotContainKey("mail.smtp.ssl.enable");
    }

    private static EmailSettings config(boolean ssl) {
        EmailSettings config = new EmailSettings();
        config.setEnabled(true);
        config.setHost("smtp.example.com");
        config.setPort(465);
        config.setUsername("sender@example.com");
        config.setPassword("secret");
        config.setFromName("Nexora Admin");
        config.setSsl(ssl);
        return config;
    }
}
