package com.nexora.system.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.system.constants.SystemConfigConstants;
import com.nexora.system.constants.SysConfigGroupEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class SysConfigGroupRegistry {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public SysConfigGroupRegistry(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.validator = validator;
    }

    public Set<String> supportedCodes() {
        return SysConfigGroupEnum.codes();
    }

    public String normalizeCode(String groupCode) {
        return normalizeType(groupCode).getCode();
    }

    private SysConfigGroupEnum normalizeType(String groupCode) {
        String normalized = groupCode == null ? null : groupCode.strip();
        SysConfigGroupEnum groupType = SysConfigGroupEnum.getByCode(normalized);
        if (groupType == null) {
            throw new BizException(SystemConfigConstants.CONFIG_GROUP_UNSUPPORTED_MESSAGE);
        }
        return groupType;
    }

    public Object parse(String groupCode, String configValue) {
        SysConfigGroupEnum groupType = normalizeType(groupCode);
        String normalizedCode = groupType.getCode();
        if (configValue == null || configValue.isBlank()) {
            throw invalid(normalizedCode, SystemConfigConstants.CONFIG_GROUP_VALUE_REQUIRED_MESSAGE);
        }
        try {
            Object config = objectMapper.readValue(configValue, groupType.getConfigType());
            validate(normalizedCode, config);
            return config;
        } catch (JsonProcessingException exception) {
            throw invalid(normalizedCode, SystemConfigConstants.CONFIG_GROUP_JSON_INVALID_MESSAGE);
        }
    }

    public <T> T parse(String groupCode, String configValue, Class<T> valueType) {
        Object config = parse(groupCode, configValue);
        if (!valueType.isInstance(config)) {
            throw invalid(groupCode, SystemConfigConstants.CONFIG_GROUP_TYPE_MISMATCH_MESSAGE);
        }
        return valueType.cast(config);
    }

    public NormalizedConfig normalize(String groupCode, JsonNode configValue) {
        SysConfigGroupEnum groupType = normalizeType(groupCode);
        String normalizedCode = groupType.getCode();
        if (configValue == null || !configValue.isObject()) {
            throw invalid(normalizedCode, SystemConfigConstants.CONFIG_GROUP_JSON_OBJECT_REQUIRED_MESSAGE);
        }
        try {
            Object config = objectMapper.treeToValue(configValue, groupType.getConfigType());
            validate(normalizedCode, config);
            return new NormalizedConfig(config, objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException exception) {
            throw invalid(normalizedCode, SystemConfigConstants.CONFIG_GROUP_JSON_INVALID_MESSAGE);
        }
    }

    private void validate(String groupCode, Object config) {
        List<String> messages = validator.validate(config).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();
        if (!messages.isEmpty()) {
            throw invalid(groupCode, messages.getFirst());
        }
    }

    private static BizException invalid(String groupCode, String reason) {
        return new BizException(SystemConfigConstants.CONFIG_GROUP_INVALID_MESSAGE.formatted(groupCode, reason));
    }

    public record NormalizedConfig(Object value, String json) {
    }
}
