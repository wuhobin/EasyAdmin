package com.nexora.config;

import com.aurora.starter.common.utils.JsonUtil;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.cache.SysConfigCache;
import com.nexora.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class SysConfigReader {

    private final SysConfigService sysConfigService;
    private final SysConfigCache sysConfigCache;

    public String getString(String configKey, String defaultValue) {
        String value = read(configKey);
        return value == null ? defaultValue : value;
    }

    public String getRequiredString(String configKey) {
        return require(configKey);
    }

    public int getInt(String configKey, int defaultValue) {
        String value = read(configKey);
        return value == null ? defaultValue : convert(configKey, "整数", value, Integer::parseInt);
    }

    public int getRequiredInt(String configKey) {
        return convert(configKey, "整数", require(configKey), Integer::parseInt);
    }

    public long getLong(String configKey, long defaultValue) {
        String value = read(configKey);
        return value == null ? defaultValue : convert(configKey, "长整数", value, Long::parseLong);
    }

    public long getRequiredLong(String configKey) {
        return convert(configKey, "长整数", require(configKey), Long::parseLong);
    }

    public boolean getBoolean(String configKey, boolean defaultValue) {
        String value = read(configKey);
        return value == null ? defaultValue : parseBoolean(configKey, value);
    }

    public boolean getRequiredBoolean(String configKey) {
        return parseBoolean(configKey, require(configKey));
    }

    public <T> T getJson(String configKey, Class<T> valueType, T defaultValue) {
        String value = read(configKey);
        return value == null ? defaultValue
                : convert(configKey, "JSON", value, content -> JsonUtil.parse(content, valueType));
    }

    public <T> T getRequiredJson(String configKey, Class<T> valueType) {
        return convert(configKey, "JSON", require(configKey), content -> JsonUtil.parse(content, valueType));
    }

    private String read(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        String normalizedKey = configKey.strip();
        return sysConfigCache.get(normalizedKey,
                () -> sysConfigService.getValueByConfigKey(normalizedKey));
    }

    private String require(String configKey) {
        String value = read(configKey);
        if (value == null) {
            throw new BizException("缺少必填配置：" + configKey);
        }
        return value;
    }

    private static boolean parseBoolean(String configKey, String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw invalidFormat(configKey, "布尔值");
    }

    private static <T> T convert(String configKey, String targetType, String value,
                                 Function<String, T> converter) {
        try {
            return converter.apply(value);
        } catch (RuntimeException exception) {
            throw invalidFormat(configKey, targetType);
        }
    }

    private static BizException invalidFormat(String configKey, String targetType) {
        return new BizException("配置项 " + configKey + " 无法转换为" + targetType);
    }
}
