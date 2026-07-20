package com.aurora.mail;

import com.aurora.handler.mail.MailCredentialCipher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailCredentialCipherTest {

    @Test
    void encryptsWithRandomNonceAndDecryptsAuthorizationCode() {
        MailCredentialCipher cipher = new MailCredentialCipher("test-mail-secret-123456");

        String first = cipher.encrypt("qq-authorization-code");
        String second = cipher.encrypt("qq-authorization-code");

        assertThat(first).startsWith("v1:").isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("qq-authorization-code");
        assertThat(cipher.decrypt(second)).isEqualTo("qq-authorization-code");
    }
}
