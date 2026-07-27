package com.nexora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.nexora.domain.form.query.system.SysConfigQueryForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysConfigControllerTest {

    @Test
    void listRequiresConfigurationViewPermission() throws Exception {
        SaCheckPermission permission = SysConfigController.class
                .getDeclaredMethod("list", SysConfigQueryForm.class, PageParam.class)
                .getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly("sys:config:list");
    }
}
