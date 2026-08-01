package com.nexora.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexora.annotation.OperationLogger;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;

class SysConfigGroupControllerTest {

    @Test
    void listRequiresConfigurationViewPermission() throws Exception {
        SaCheckPermission permission = SysConfigGroupController.class
                .getDeclaredMethod("list")
                .getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly("sys:config:list");
    }

    @Test
    void publicConfigurationDoesNotRequireAuthenticationPermission() throws Exception {
        var method = SysConfigGroupController.class.getDeclaredMethod("publicConfig");
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/public");
        assertThat(method.getAnnotation(SaCheckPermission.class)).isNull();
    }

    @Test
    void updateUsesTheDynamicGroupPathAndOnlyTheUpdatePermission() throws Exception {
        var method = SysConfigGroupController.class
                .getDeclaredMethod("update", String.class, JsonNode.class);
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(mapping.value()).containsExactly("/{groupCode}");
        assertThat(permission.value()).containsExactly("sys:config:update");
        assertThat(method.getAnnotation(OperationLogger.class)).isNull();
    }

    @Test
    void testEmailRequiresConfigurationUpdatePermission() throws Exception {
        var method = SysConfigGroupController.class.getDeclaredMethod(
                "testEmail", String.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        var parameter = method.getParameters()[0];

        assertThat(mapping.value()).containsExactly("/test-email");
        assertThat(permission.value()).containsExactly("sys:config:update");
        assertThat(parameter.isAnnotationPresent(RequestParam.class)).isTrue();
        assertThat(parameter.isAnnotationPresent(RequestBody.class)).isFalse();
    }
}
