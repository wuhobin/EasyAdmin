package com.nexora.system.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.system.api.WechatLoginSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class WechatConfigSecretService {

    public static final String MASK = "******";
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private final ObjectMapper objectMapper;
    private final String masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public WechatConfigSecretService(
            ObjectMapper objectMapper,
            @Value("${NEXORA_CONFIG_ENCRYPTION_KEY:}") String masterKey) {
        this.objectMapper = objectMapper;
        this.masterKey = masterKey;
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
        result.setAppSecret(decryptValue(stored.getAppSecret()));
        result.setToken(decryptValue(stored.getToken()));
        result.setAesKey(decryptValue(stored.getAesKey()));
        return result;
    }

    public WechatLoginSettings prepareForStorage(WechatLoginSettings incoming, WechatLoginSettings existing) {
        WechatLoginSettings result = copy(incoming);
        result.setAppSecret(mergeAndEncrypt(incoming.getAppSecret(), existing.getAppSecret()));
        result.setToken(mergeAndEncrypt(incoming.getToken(), existing.getToken()));
        result.setAesKey(mergeAndEncrypt(incoming.getAesKey(), existing.getAesKey()));
        return result;
    }

    public String toJson(WechatLoginSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("微信登录配置序列化失败", exception);
        }
    }

    private String mergeAndEncrypt(String incoming, String existing) {
        if (incoming == null || incoming.isBlank() || MASK.equals(incoming)) {
            return encryptValue(existing);
        }
        return encryptValue(incoming.strip());
    }

    private String encryptValue(String value) {
        if (value == null || value.isBlank() || value.startsWith(PREFIX)) {
            return value;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("微信登录配置加密失败", exception);
        }
    }

    private String decryptValue(String value) {
        if (value == null || value.isBlank() || !value.startsWith(PREFIX)) {
            return value;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("微信登录配置解密失败", exception);
        }
    }

    private SecretKeySpec key() {
        byte[] decoded = Base64.getDecoder().decode(masterKey);
        if (decoded.length != 32) {
            throw new IllegalStateException("NEXORA_CONFIG_ENCRYPTION_KEY 必须是 Base64 编码的32字节密钥");
        }
        return new SecretKeySpec(decoded, "AES");
    }

    private WechatLoginSettings copy(WechatLoginSettings source) {
        return objectMapper.convertValue(source, WechatLoginSettings.class);
    }

    private static String maskValue(String value) {
        return value == null || value.isBlank() ? "" : MASK;
    }
}
