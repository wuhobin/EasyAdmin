package com.nexora.security;

import com.aurora.starter.webmvc.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts credential-bearing fields for every business domain with one platform key.
 *
 * <p>The purpose is authenticated as additional data so ciphertext cannot be moved between
 * unrelated fields such as a mail authorization code and an SSH password.</p>
 */
@Component
public class PlatformCredentialCipher {

    private static final String VERSION = "v1";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public PlatformCredentialCipher(
            @Value("${nexora.security.credential-secret:}") String encodedSecret) {
        this.key = new SecretKeySpec(decodeKey(encodedSecret), "AES");
    }

    public String encrypt(String purpose, String value) {
        requirePurpose(purpose);
        if (value == null) {
            throw new IllegalArgumentException("Credential value must not be null");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new BizException("平台凭据加密失败");
        }
    }

    public String decrypt(String purpose, String value) {
        requirePurpose(purpose);
        try {
            String[] parts = value.split(":", 3);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported credential ciphertext");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            if (iv.length != IV_LENGTH) {
                throw new IllegalArgumentException("Invalid credential IV");
            }
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new BizException("平台凭据解密失败，请检查 PLATFORM_CREDENTIAL_SECRET 是否一致");
        }
    }

    private static byte[] decodeKey(String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException(
                    "必须配置 Base64 编码的 32 字节 PLATFORM_CREDENTIAL_SECRET");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedSecret.trim());
            if (decoded.length != KEY_LENGTH) {
                throw new IllegalStateException(
                        "PLATFORM_CREDENTIAL_SECRET 解码后必须恰好为 32 字节");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "PLATFORM_CREDENTIAL_SECRET 必须是有效的 Base64 编码", exception);
        }
    }

    private static void requirePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Credential purpose must not be blank");
        }
    }
}
