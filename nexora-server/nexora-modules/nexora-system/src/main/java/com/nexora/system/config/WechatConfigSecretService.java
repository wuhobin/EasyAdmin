package com.nexora.system.config;

import com.aurora.starter.webmvc.security.PlatformCredentialCipher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.system.api.WechatLoginSettings;
import org.springframework.stereotype.Component;

@Component
public class WechatConfigSecretService {

    public static final String MASK = "******";
    private static final String CIPHERTEXT_PREFIX = "v1:";
    private static final String APP_SECRET_PURPOSE = "system.wechat.app-secret";
    private static final String TOKEN_PURPOSE = "system.wechat.token";
    private static final String AES_KEY_PURPOSE = "system.wechat.aes-key";
    private final ObjectMapper objectMapper;
    private final PlatformCredentialCipher credentialCipher;

    public WechatConfigSecretService(
            ObjectMapper objectMapper,
            PlatformCredentialCipher credentialCipher) {
        this.objectMapper = objectMapper;
        this.credentialCipher = credentialCipher;
    }

    public WechatLoginSettings mask(WechatLoginSettings stored) {
        WechatLoginSettings result = copy(stored);
        result.setAppSecret(maskValue(stored.getAppSecret()));
        result.setToken(maskValue(stored.getToken()));
        result.setAesKey(maskValue(stored.getAesKey()));
        return result;
    }

    public WechatLoginSettings decrypt(WechatLoginSettings stored) {
        WechatLoginSettings result = copy(stored);
        result.setAppSecret(decryptValue(APP_SECRET_PURPOSE, stored.getAppSecret()));
        result.setToken(decryptValue(TOKEN_PURPOSE, stored.getToken()));
        result.setAesKey(decryptValue(AES_KEY_PURPOSE, stored.getAesKey()));
        return result;
    }

    public WechatLoginSettings prepareForStorage(WechatLoginSettings incoming, WechatLoginSettings existing) {
        WechatLoginSettings result = copy(incoming);
        result.setAppSecret(mergeAndEncrypt(APP_SECRET_PURPOSE, incoming.getAppSecret(), existing.getAppSecret()));
        result.setToken(mergeAndEncrypt(TOKEN_PURPOSE, incoming.getToken(), existing.getToken()));
        result.setAesKey(mergeAndEncrypt(AES_KEY_PURPOSE, incoming.getAesKey(), existing.getAesKey()));
        return result;
    }

    public String toJson(WechatLoginSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("微信登录配置序列化失败", exception);
        }
    }

    private String mergeAndEncrypt(String purpose, String incoming, String existing) {
        if (incoming == null || incoming.isBlank() || MASK.equals(incoming)) {
            return encryptExistingValue(purpose, existing);
        }
        return credentialCipher.encrypt(purpose, incoming.strip());
    }

    private String encryptExistingValue(String purpose, String value) {
        if (value == null || value.isBlank() || isCiphertext(value)) {
            return value;
        }
        return credentialCipher.encrypt(purpose, value);
    }

    private String decryptValue(String purpose, String value) {
        if (value == null || value.isBlank() || !isCiphertext(value)) {
            return value;
        }
        return credentialCipher.decrypt(purpose, value);
    }

    private static boolean isCiphertext(String value) {
        return value.startsWith(CIPHERTEXT_PREFIX);
    }

    private WechatLoginSettings copy(WechatLoginSettings source) {
        return objectMapper.convertValue(source, WechatLoginSettings.class);
    }

    private static String maskValue(String value) {
        return value == null || value.isBlank() ? "" : MASK;
    }
}
