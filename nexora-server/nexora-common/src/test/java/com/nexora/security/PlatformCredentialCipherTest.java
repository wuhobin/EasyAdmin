package com.nexora.security;

import com.aurora.starter.webmvc.exception.BizException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformCredentialCipherTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void encryptsWithRandomNonceAndDecryptsForTheSamePurpose() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(SECRET);

        String first = cipher.encrypt("mail.auth-code", "credential");
        String second = cipher.encrypt("mail.auth-code", "credential");

        assertThat(first).startsWith("v1:").isNotEqualTo(second);
        assertThat(cipher.decrypt("mail.auth-code", first)).isEqualTo("credential");
        assertThat(cipher.decrypt("mail.auth-code", second)).isEqualTo("credential");
    }

    @Test
    void rejectsCiphertextFromAnotherPurpose() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(SECRET);
        String encrypted = cipher.encrypt("mail.auth-code", "credential");

        assertThatThrownBy(() -> cipher.decrypt("monitor.ssh-password", encrypted))
                .isInstanceOf(BizException.class);
    }

    @Test
    void rejectsMissingOrInvalidKeys() {
        assertThatThrownBy(() -> new PlatformCredentialCipher(""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PlatformCredentialCipher(
                Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }
}
