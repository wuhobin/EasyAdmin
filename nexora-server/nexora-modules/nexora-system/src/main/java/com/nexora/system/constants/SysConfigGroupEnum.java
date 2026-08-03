package com.nexora.system.constants;

import com.nexora.system.domain.form.EmailConfigForm;
import com.nexora.system.domain.form.LoginConfigForm;
import com.nexora.system.domain.form.PasswordConfigForm;
import com.nexora.system.domain.form.RegisterConfigForm;
import com.nexora.system.domain.form.SystemConfigForm;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum SysConfigGroupEnum {
    SYSTEM("system", "系统配置", SystemConfigForm.class),
    REGISTER("register", "注册配置", RegisterConfigForm.class),
    LOGIN("login", "登录配置", LoginConfigForm.class),
    PASSWORD("password", "密码配置", PasswordConfigForm.class),
    EMAIL("email", "邮箱配置", EmailConfigForm.class);

    private static final Set<String> CODES = Arrays.stream(values())
            .map(SysConfigGroupEnum::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private final String code;
    private final String description;
    private final Class<?> configType;

    SysConfigGroupEnum(String code, String description, Class<?> configType) {
        this.code = code;
        this.description = description;
        this.configType = configType;
    }

    public static SysConfigGroupEnum getByCode(String code) {
        for (SysConfigGroupEnum group : values()) {
            if (group.getCode().equals(code)) {
                return group;
            }
        }
        return null;
    }

    public static Set<String> codes() {
        return CODES;
    }
}
