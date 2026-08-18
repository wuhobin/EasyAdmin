package com.nexora.system.config;

import com.aurora.starter.webmvc.security.PlatformCredentialCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.system.api.WechatLoginSettings;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class WechatConfigSecretServiceTest {

    private final WechatConfigSecretService service = new WechatConfigSecretService(
            new ObjectMapper(), new PlatformCredentialCipher(
                    Base64.getEncoder().encodeToString(new byte[32])));

    @Test
    void encryptsSecretsAtRestAndMasksAdminDetails() {
        WechatLoginSettings incoming = settings("app-secret", "callback-token", "encoding-key");
        WechatLoginSettings stored = service.prepareForStorage(incoming, settings("", "", ""));

        assertThat(stored.getAppSecret()).startsWith("v1:").doesNotContain("app-secret");
        assertThat(stored.getToken()).startsWith("v1:").doesNotContain("callback-token");
        assertThat(stored.getAesKey()).startsWith("v1:").doesNotContain("encoding-key");
        assertThat(service.decrypt(stored))
                .extracting(WechatLoginSettings::getAppSecret, WechatLoginSettings::getToken,
                        WechatLoginSettings::getAesKey)
                .containsExactly("app-secret", "callback-token", "encoding-key");
        assertThat(service.mask(stored))
                .extracting(WechatLoginSettings::getAppSecret, WechatLoginSettings::getToken,
                        WechatLoginSettings::getAesKey)
                .containsExactly(WechatConfigSecretService.MASK, WechatConfigSecretService.MASK,
                        WechatConfigSecretService.MASK);
    }

    @Test
    void blankAdminSecretsPreserveExistingCiphertext() {
        WechatLoginSettings existing = service.prepareForStorage(
                settings("old-secret", "old-token", "old-key"), settings("", "", ""));
        WechatLoginSettings stored = service.prepareForStorage(settings("", "", ""), existing);

        assertThat(stored.getAppSecret()).isEqualTo(existing.getAppSecret());
        assertThat(stored.getToken()).isEqualTo(existing.getToken());
        assertThat(stored.getAesKey()).isEqualTo(existing.getAesKey());
    }

    @Test
    void blankAdminSecretsEncryptLegacyPlaintextValues() {
        WechatLoginSettings stored = service.prepareForStorage(
                settings("", "", ""), settings("old-secret", "old-token", "old-key"));

        assertThat(stored.getAppSecret()).startsWith("v1:").doesNotContain("old-secret");
        assertThat(stored.getToken()).startsWith("v1:").doesNotContain("old-token");
        assertThat(stored.getAesKey()).startsWith("v1:").doesNotContain("old-key");
        assertThat(service.decrypt(stored))
                .extracting(WechatLoginSettings::getAppSecret, WechatLoginSettings::getToken,
                        WechatLoginSettings::getAesKey)
                .containsExactly("old-secret", "old-token", "old-key");
    }

    private static WechatLoginSettings settings(String secret, String token, String aesKey) {
        WechatLoginSettings settings = new WechatLoginSettings();
        settings.setEnabled(true);
        settings.setQrCodeUrl("https://example.com/qr.png");
        settings.setAppId("wx-app");
        settings.setAppSecret(secret);
        settings.setToken(token);
        settings.setAesKey(aesKey);
        return settings;
    }
}
