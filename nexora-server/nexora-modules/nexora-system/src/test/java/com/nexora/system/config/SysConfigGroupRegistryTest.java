package com.nexora.system.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemSettings;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SysConfigGroupRegistryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final SysConfigGroupRegistry registry = new SysConfigGroupRegistry(objectMapper, validator);

    @Test
    void exposesOnlyTheFixedGroups() {
        assertThat(registry.supportedCodes())
                .containsExactlyInAnyOrder("system", "register", "login", "password", "email", "wechat");
    }

    @Test
    void normalizesTheGroupCodeAndStringValues() throws Exception {
        ObjectNode input = validSystemConfig();
        input.put("siteName", "  Nexora Admin  ");

        SysConfigGroupRegistry.NormalizedConfig normalized = registry.normalize(" system ", input);

        assertThat(normalized.value()).isInstanceOf(SystemSettings.class);
        assertThat(((SystemSettings) normalized.value()).getSiteName()).isEqualTo("Nexora Admin");
        assertThat(objectMapper.readTree(normalized.json()).get("siteName").asText())
                .isEqualTo("Nexora Admin");
    }

    @Test
    void rejectsUnknownJsonFields() {
        ObjectNode input = validSystemConfig();
        input.put("unexpected", true);

        assertThatThrownBy(() -> registry.normalize("system", input))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("配置 JSON 格式或字段不正确");
    }

    @Test
    void rejectsInvalidCrossFieldRules() {
        ObjectNode input = objectMapper.createObjectNode()
                .put("minLength", 20)
                .put("maxLength", 10)
                .put("requireUppercase", false)
                .put("requireLowercase", false)
                .put("requireNumber", false)
                .put("requireSpecial", false);

        assertThatThrownBy(() -> registry.normalize("password", input))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("密码最大长度不能小于最小长度");
    }

    @Test
    void requiresTheRegistrationCaptchaSwitch() {
        ObjectNode input = objectMapper.createObjectNode()
                .put("verifyEmail", true)
                .put("defaultRoleCode", "user")
                .put("needAudit", false);

        assertThatThrownBy(() -> registry.normalize("register", input))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("注册滑块验证开关不能为空");
    }

    @Test
    void parsesTheRegistrationCaptchaSwitchIndependentlyFromEmailVerification() {
        ObjectNode input = objectMapper.createObjectNode()
                .put("captchaEnabled", false)
                .put("verifyEmail", true)
                .put("defaultRoleCode", "user")
                .put("needAudit", false);

        RegistrationSettings config = (RegistrationSettings) registry.normalize("register", input).value();

        assertThat(config.getCaptchaEnabled()).isFalse();
        assertThat(config.getVerifyEmail()).isTrue();
    }

    @Test
    void requiresSmtpConnectionFieldsOnlyWhenEmailIsEnabled() {
        ObjectNode input = objectMapper.createObjectNode()
                .put("enabled", true)
                .put("host", "")
                .put("port", 465)
                .put("username", "")
                .put("password", "")
                .put("fromName", "Nexora Admin")
                .put("ssl", true);

        assertThatThrownBy(() -> registry.normalize("email", input))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("启用邮件时必须填写SMTP服务器、用户名和密码");

        input.put("enabled", false);
        assertThat(registry.normalize("email", input).json())
                .doesNotContain("connectionCompleteWhenEnabled");
    }

    @Test
    void rejectsUnsupportedGroups() {
        assertThatThrownBy(() -> registry.normalizeCode("mail"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持的配置分组");
    }

    private ObjectNode validSystemConfig() {
        return objectMapper.createObjectNode()
                .put("siteName", "Nexora Admin")
                .put("shortTitle", "Nexora")
                .put("siteDescription", "后台管理系统")
                .put("siteLogo", "")
                .put("copyright", "Copyright")
                .put("icp", "")
                .put("watermarkEnabled", false)
                .put("watermarkType", "username_time")
                .put("watermarkCustomText", "")
                .put("watermarkOpacity", 0.15);
    }
}
