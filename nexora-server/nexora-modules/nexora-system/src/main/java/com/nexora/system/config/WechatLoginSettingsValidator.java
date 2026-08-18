package com.nexora.system.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.system.api.SystemSettingsValidator;
import com.nexora.system.api.WechatLoginSettings;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class WechatLoginSettingsValidator implements SystemSettingsValidator {

    @Override
    public boolean supports(String groupCode) {
        return WechatLoginSettings.GROUP_CODE.equals(groupCode);
    }

    @Override
    public void validate(Object config) {
        WechatLoginSettings settings = (WechatLoginSettings) config;
        if (!Boolean.TRUE.equals(settings.getEnabled())) {
            return;
        }
        require(settings.getQrCodeUrl(), "启用微信登录时必须配置公众号二维码地址");
        require(settings.getAppId(), "启用微信登录时必须配置 AppID");
        require(settings.getAppSecret(), "启用微信登录时必须配置 AppSecret");
        require(settings.getToken(), "启用微信登录时必须配置 Token");
        require(settings.getAesKey(), "启用微信登录时必须配置 EncodingAESKey");
        try {
            URI uri = URI.create(settings.getQrCodeUrl());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new BizException("公众号二维码地址必须是有效的 HTTP(S) URL");
        }
    }

    private static void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(message);
        }
    }
}
