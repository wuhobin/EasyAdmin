package com.nexora.monitor.aspect;

import com.nexora.annotation.OperationLogger;
import com.nexora.monitor.entity.SysOperateLog;
import com.nexora.monitor.infrastructure.IpRegionUtils;
import com.nexora.monitor.infrastructure.OperationLogContext;
import com.nexora.monitor.mapper.SysOperateLogMapper;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Test
    void capturesTheOperatorBeforeSelfLogoutAndMergesServerResolvedTarget() throws Throwable {
        String sessionId = "550e8400-e29b-41d4-a716-446655440000";
        AtomicBoolean sessionInvalidated = new AtomicBoolean();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        OperationLogger annotation = mock(OperationLogger.class);
        SysOperateLogMapper mapper = mock(SysOperateLogMapper.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{sessionId});
        when(signature.getParameterNames()).thenReturn(new String[]{"sessionId"});
        when(signature.getName()).thenReturn("forceLogout");
        when(annotation.save()).thenReturn(true);
        when(annotation.value()).thenReturn("强退在线会话 {1}");
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            sessionInvalidated.set(true);
            OperationLogContext.setTarget(
                    42,
                    "target@example.com",
                    sessionId,
                    "203.0.113.8");
            OperationLogContext.setOutcome("LOGGED_OUT");
            return "logged-out";
        });
        MockHttpServletRequest request =
                new MockHttpServletRequest("DELETE", "/monitor/online/" + sessionId);
        OperationLoggerAspect aspect = new OperationLoggerAspect(mapper);

        try (MockedStatic<ServletUtils> servletUtils = mockStatic(ServletUtils.class);
             MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
             MockedStatic<IpRegionUtils> ipRegion = mockStatic(IpRegionUtils.class)) {
            servletUtils.when(ServletUtils::getRequest).thenReturn(request);
            servletUtils.when(() -> ServletUtils.getClientIp(request))
                    .thenReturn("198.51.100.9");
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenAnswer(invocation -> {
                assertThat(sessionInvalidated).isFalse();
                return 7;
            });
            ipRegion.when(() -> IpRegionUtils.resolve("198.51.100.9"))
                    .thenReturn("operator-location");

            assertThat(aspect.doAround(joinPoint, annotation))
                    .isEqualTo("logged-out");

            securityUtils.verify(SecurityUtils::getLoginIdAsInt, times(1));
        }

        ArgumentCaptor<SysOperateLog> captor =
                ArgumentCaptor.forClass(SysOperateLog.class);
        verify(mapper).insert(captor.capture());
        SysOperateLog log = captor.getValue();
        assertThat(log.getUserId()).isEqualTo(7);
        assertThat(log.getIp()).isEqualTo("198.51.100.9");
        assertThat(log.getSource()).isEqualTo("operator-location");
        assertThat(log.getRequestUrl()).isEqualTo("/monitor/online/" + sessionId);
        assertThat(log.getType()).isEqualTo("DELETE");
        assertThat(log.getOperationName())
                .isEqualTo("强退在线会话 \"" + sessionId + "\"");
        assertThat(log.getParamsJson())
                .contains("\"sessionId\":\"" + sessionId + "\"")
                .contains("\"targetUserId\":42")
                .contains("\"targetEmail\":\"target@example.com\"")
                .contains("\"targetSessionId\":\"" + sessionId + "\"")
                .contains("\"targetIp\":\"203.0.113.8\"")
                .contains("\"outcome\":\"LOGGED_OUT\"")
                .doesNotContain("token", "credential");
        assertThat(OperationLogContext.parameters()).isEmpty();
    }

    @Test
    void clearsAuditContextWhenTheBusinessOperationFails() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        OperationLogger annotation = mock(OperationLogger.class);
        SysOperateLogMapper mapper = mock(SysOperateLogMapper.class);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            OperationLogContext.setTarget(
                    42,
                    "target@example.com",
                    "550e8400-e29b-41d4-a716-446655440000",
                    "203.0.113.8");
            throw new IllegalStateException("force logout failed");
        });
        OperationLoggerAspect aspect = new OperationLoggerAspect(mapper);

        try (MockedStatic<ServletUtils> servletUtils = mockStatic(ServletUtils.class);
             MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            servletUtils.when(ServletUtils::getRequest)
                    .thenReturn(new MockHttpServletRequest());

            assertThatThrownBy(() -> aspect.doAround(joinPoint, annotation))
                    .isInstanceOf(IllegalStateException.class);
        }

        assertThat(OperationLogContext.parameters()).isEmpty();
        verify(mapper, never()).insert(
                org.mockito.ArgumentMatchers.any(SysOperateLog.class));
    }

    @Test
    void doesNotRollBackTheBusinessResultWhenLogPersistenceFails() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        OperationLogger annotation = mock(OperationLogger.class);
        SysOperateLogMapper mapper = mock(SysOperateLogMapper.class);
        when(joinPoint.proceed()).thenReturn("logged-out");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(signature.getParameterNames()).thenReturn(new String[0]);
        when(signature.getName()).thenReturn("forceLogout");
        when(annotation.save()).thenReturn(true);
        when(annotation.value()).thenReturn("强退在线会话");
        when(mapper.insert(org.mockito.ArgumentMatchers.any(SysOperateLog.class)))
                .thenThrow(new IllegalStateException("database unavailable"));
        OperationLoggerAspect aspect = new OperationLoggerAspect(mapper);

        try (MockedStatic<ServletUtils> servletUtils = mockStatic(ServletUtils.class);
             MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
             MockedStatic<IpRegionUtils> ipRegion = mockStatic(IpRegionUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "DELETE", "/monitor/online/session");
            servletUtils.when(ServletUtils::getRequest).thenReturn(request);
            servletUtils.when(() -> ServletUtils.getClientIp(request))
                    .thenReturn("127.0.0.1");

            assertThat(aspect.doAround(joinPoint, annotation))
                    .isEqualTo("logged-out");
        }

        assertThat(OperationLogContext.parameters()).isEmpty();
    }
}
