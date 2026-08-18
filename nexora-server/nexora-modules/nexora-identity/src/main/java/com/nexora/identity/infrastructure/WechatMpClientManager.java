package com.nexora.identity.infrastructure;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.system.api.SystemConfigReader;
import com.nexora.system.api.WechatLoginSettings;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WechatMpClientManager {

    private final SystemConfigReader configReader;
    private volatile CachedClient cachedClient;

    public WechatLoginSettings requireSettings() {
        WechatLoginSettings settings = configReader.wechat();
        if (!Boolean.TRUE.equals(settings.getEnabled())) {
            throw new BizException("微信公众号登录未启用");
        }
        return settings;
    }

    public WxMpService service() {
        WechatLoginSettings settings = requireSettings();
        String fingerprint = String.join("\u0000", settings.getAppId(), settings.getAppSecret(),
                settings.getToken(), settings.getAesKey());
        CachedClient current = cachedClient;
        if (current != null && current.fingerprint().equals(fingerprint)) {
            return current.service();
        }
        synchronized (this) {
            current = cachedClient;
            if (current == null || !current.fingerprint().equals(fingerprint)) {
                WxMpDefaultConfigImpl storage = new WxMpDefaultConfigImpl();
                storage.setAppId(settings.getAppId());
                storage.setSecret(settings.getAppSecret());
                storage.setToken(settings.getToken());
                storage.setAesKey(settings.getAesKey());
                WxMpServiceImpl service = new WxMpServiceImpl();
                service.setWxMpConfigStorage(storage);
                current = new CachedClient(fingerprint, service);
                cachedClient = current;
            }
        }
        return current.service();
    }

    private record CachedClient(String fingerprint, WxMpService service) {
    }
}
