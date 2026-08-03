package com.nexora.monitor.aspect;

import com.nexora.contract.OperationLogger;
import com.nexora.monitor.mapper.SysOperateLogMapper;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLoggerAspectTest {

    @Test
    void resolvesOneBasedOperationNamePlaceholders() {
        String name = OperationLoggerAspect.formatOperationName(
                "删除用户 {1}", new Object[]{List.of(1, 2)});

        assertThat(name).isEqualTo("删除用户 [1,2]");
    }

    @Test
    void filtersServletAndMultipartArgumentsFromParameters() {
        HttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", "text/plain", "a".getBytes());

        String json = OperationLoggerAspect.serializeParameters(
                new String[]{"name", "request", "file"},
                new Object[]{"admin", request, file});

        assertThat(json).isEqualTo("{\"name\":\"admin\"}");
    }

    @Test
    void doesNotStoreRequestTimingInAspectInstanceField() {
        boolean containsStartTimeField = Arrays.stream(OperationLoggerAspect.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("startTime"));

        assertThat(containsStartTimeField).isFalse();
    }

    @Test
    void doesNotRequireTheAdminRoleBeforeProceeding() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        OperationLogger annotation = mock(OperationLogger.class);
        when(joinPoint.proceed()).thenReturn("ok");
        when(annotation.save()).thenReturn(false);
        OperationLoggerAspect aspect = new OperationLoggerAspect(mock(SysOperateLogMapper.class));

        try (MockedStatic<ServletUtils> servletUtils = mockStatic(ServletUtils.class);
             MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            servletUtils.when(ServletUtils::getRequest).thenReturn(new MockHttpServletRequest());

            assertThat(aspect.doAround(joinPoint, annotation)).isEqualTo("ok");

            securityUtils.verify(SecurityUtils::checkLogin);
            verify(joinPoint).proceed();
        }
    }

    @Test
    void recordsTheCurrentLoginIdWithoutDependingOnIdentitySessionData() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        OperationLogger annotation = mock(OperationLogger.class);
        SysOperateLogMapper mapper = mock(SysOperateLogMapper.class);
        when(joinPoint.proceed()).thenReturn("ok");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(signature.getParameterNames()).thenReturn(new String[0]);
        when(signature.getName()).thenReturn("update");
        when(annotation.save()).thenReturn(true);
        when(annotation.value()).thenReturn("更新用户");
        OperationLoggerAspect aspect = new OperationLoggerAspect(mapper);

        try (MockedStatic<ServletUtils> servletUtils = mockStatic(ServletUtils.class);
             MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/sys/user/7");
            servletUtils.when(ServletUtils::getRequest).thenReturn(request);
            servletUtils.when(() -> ServletUtils.getClientIp(request)).thenReturn("127.0.0.1");
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            assertThat(aspect.doAround(joinPoint, annotation)).isEqualTo("ok");
        }

        org.mockito.ArgumentCaptor<com.nexora.monitor.entity.SysOperateLog> captor =
                org.mockito.ArgumentCaptor.forClass(com.nexora.monitor.entity.SysOperateLog.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7);
    }
}
