package com.nexora.constants;

import com.nexora.domain.form.system.config.EmailConfigForm;
import com.nexora.domain.form.system.config.LoginConfigForm;
import com.nexora.domain.form.system.config.PasswordConfigForm;
import com.nexora.domain.form.system.config.RegisterConfigForm;
import com.nexora.domain.form.system.config.SystemConfigForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysConfigGroupEnumTest {

    @Test
    void managesFixedGroupCodesAndConfigurationTypes() {
        assertThat(SysConfigGroupEnum.SYSTEM)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("system", "系统配置", SystemConfigForm.class);
        assertThat(SysConfigGroupEnum.REGISTER)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("register", "注册配置", RegisterConfigForm.class);
        assertThat(SysConfigGroupEnum.LOGIN)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("login", "登录配置", LoginConfigForm.class);
        assertThat(SysConfigGroupEnum.PASSWORD)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("password", "密码配置", PasswordConfigForm.class);
        assertThat(SysConfigGroupEnum.EMAIL)
                .extracting(SysConfigGroupEnum::getCode, SysConfigGroupEnum::getDescription,
                        SysConfigGroupEnum::getConfigType)
                .containsExactly("email", "邮箱配置", EmailConfigForm.class);
    }

    @Test
    void resolvesAGroupByCode() {
        assertThat(SysConfigGroupEnum.getByCode("register")).isEqualTo(SysConfigGroupEnum.REGISTER);
        assertThat(SysConfigGroupEnum.getByCode("unknown")).isNull();
        assertThat(SysConfigGroupEnum.codes())
                .containsExactlyInAnyOrder("system", "register", "login", "password", "email");
    }
}
