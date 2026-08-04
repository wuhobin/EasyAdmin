package com.nexora.system.constants;

import com.nexora.system.api.EmailSettings;
import com.nexora.system.api.LoginSettings;
import com.nexora.system.api.PasswordSettings;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemSettings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysConfigGroupEnumTest {

    @Test
    void managesFixedGroupCodesAndConfigurationTypes() {
        assertThat(SysConfigGroupEnum.SYSTEM)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("system", "系统配置", SystemSettings.class);
        assertThat(SysConfigGroupEnum.REGISTER)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("register", "注册配置", RegistrationSettings.class);
        assertThat(SysConfigGroupEnum.LOGIN)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("login", "登录配置", LoginSettings.class);
        assertThat(SysConfigGroupEnum.PASSWORD)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("password", "密码配置", PasswordSettings.class);
        assertThat(SysConfigGroupEnum.EMAIL)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("email", "邮箱配置", EmailSettings.class);
    }

    @Test
    void resolvesAGroupByCode() {
        assertThat(SysConfigGroupEnum.getByCode("register")).isEqualTo(SysConfigGroupEnum.REGISTER);
        assertThat(SysConfigGroupEnum.getByCode("unknown")).isNull();
        assertThat(SysConfigGroupEnum.codes())
                .containsExactlyInAnyOrder("system", "register", "login", "password", "email");
    }
}
