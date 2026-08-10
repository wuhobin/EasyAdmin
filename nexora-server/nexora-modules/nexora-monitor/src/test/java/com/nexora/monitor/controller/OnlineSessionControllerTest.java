package com.nexora.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexora.annotation.OperationLogger;
import com.nexora.monitor.biz.OnlineSessionBizService;
import com.nexora.monitor.domain.form.OnlineSessionQueryForm;
import com.nexora.monitor.domain.vo.ForceLogoutResultVo;
import com.nexora.monitor.domain.vo.OnlineSessionVo;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnlineSessionControllerTest {

    @Test
    void exposesThePermissionProtectedOnlineSessionListEndpoint() throws Exception {
        assertThat(OnlineSessionController.class
                .getAnnotation(RequestMapping.class)
                .value())
                .containsExactly("/monitor/online");
        Method method = OnlineSessionController.class
                .getMethod("list", OnlineSessionQueryForm.class);
        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly("/list");
        assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("sys:online");
    }

    @Test
    void delegatesTheValidatedQueryToTheBizService() {
        OnlineSessionBizService bizService = mock(OnlineSessionBizService.class);
        OnlineSessionController controller = new OnlineSessionController(bizService);
        OnlineSessionQueryForm form = new OnlineSessionQueryForm();
        Page<OnlineSessionVo> page = new Page<>(1, 10, 0);
        when(bizService.list(form)).thenReturn(page);

        var result = controller.list(form);

        assertThat(result.getData()).isSameAs(page);
        verify(bizService).list(form);
    }

    @Test
    void exposesThePermissionProtectedForceLogoutEndpoint() throws Exception {
        Method method = OnlineSessionController.class
                .getMethod("forceLogout", String.class);

        assertThat(method.getAnnotation(DeleteMapping.class).value())
                .containsExactly("/{sessionId}");
        assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("sys:online:forceLogout");
        assertThat(method.getAnnotation(OperationLogger.class).value())
                .isEqualTo("强退在线会话 {1}");
    }

    @Test
    void rejectsNonUuidSessionIdsBeforeControllerInvocation() throws Exception {
        OnlineSessionController controller =
                new OnlineSessionController(mock(OnlineSessionBizService.class));
        Method method = OnlineSessionController.class
                .getMethod("forceLogout", String.class);

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().forExecutables()
                    .validateParameters(
                            controller, method, new Object[]{"credential-value"}))
                    .hasSize(1);
        }
    }

    @Test
    void delegatesForceLogoutAndReturnsItsIdempotentOutcome() {
        OnlineSessionBizService bizService = mock(OnlineSessionBizService.class);
        OnlineSessionController controller = new OnlineSessionController(bizService);
        ForceLogoutResultVo outcome = new ForceLogoutResultVo(
                ForceLogoutResultVo.Outcome.LOGGED_OUT, true);
        when(bizService.forceLogout("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(outcome);

        var result = controller.forceLogout(
                "550e8400-e29b-41d4-a716-446655440000");

        assertThat(result.getData()).isSameAs(outcome);
        verify(bizService)
                .forceLogout("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void responseContractDoesNotExposeCredentialsOrInternalStatus() {
        assertThat(Arrays.stream(OnlineSessionVo.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("token", "tokenValue", "userId", "userAgent", "status");
        assertThat(Arrays.stream(ForceLogoutResultVo.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("token", "tokenValue", "userId", "userAgent");
    }
}
