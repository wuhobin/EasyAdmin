package com.nexora.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexora.monitor.biz.OnlineSessionBizService;
import com.nexora.monitor.domain.form.OnlineSessionQueryForm;
import com.nexora.monitor.domain.vo.OnlineSessionVo;
import org.junit.jupiter.api.Test;
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
    void responseContractDoesNotExposeCredentialsOrInternalStatus() {
        assertThat(Arrays.stream(OnlineSessionVo.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("token", "tokenValue", "userId", "userAgent", "status");
    }
}
