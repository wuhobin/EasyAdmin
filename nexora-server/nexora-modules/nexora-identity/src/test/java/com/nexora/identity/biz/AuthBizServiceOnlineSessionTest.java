package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.nexora.identity.cache.LoginRetryCache;
import com.nexora.identity.domain.form.AuthForm;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.security.NexoraPermissionProvider;
import com.nexora.identity.service.SysUserService;
import com.nexora.system.api.LoginSettings;
import com.nexora.system.api.SystemConfigReader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthBizServiceOnlineSessionTest {

    private static final String FIRST_SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SECOND_SESSION_ID = "550e8400-e29b-41d4-a716-446655440001";

    private final SysUserService userService = mock(SysUserService.class);
    private final SystemConfigReader configReader = mock(SystemConfigReader.class);
    private final OnlineSessionLifecycleService onlineSessionLifecycleService =
            mock(OnlineSessionLifecycleService.class);
    private final AuthBizService service = new AuthBizService(
            userService,
            mock(NexoraPermissionProvider.class),
            configReader,
            new LoginSecurityService(mock(LoginRetryCache.class)),
            mock(ImageVerificationService.class),
            mock(RegistrationService.class),
            mock(PasswordResetService.class),
            mock(MailVerificationOrchestrator.class),
            onlineSessionLifecycleService);

    @Test
    void createsAnIndependentDeviceIdAndRegistryRecordForEveryLogin() {
        SysUser user = SysUser.builder()
                .id(7)
                .email("user@example.com")
                .nickname("User")
                .password(BCrypt.hashpw("secret", BCrypt.gensalt()))
                .status(1)
                .build();
        when(userService.getByEmail("user@example.com")).thenReturn(user);
        when(configReader.login()).thenReturn(loginConfig());
        when(onlineSessionLifecycleService.createSessionId())
                .thenReturn(FIRST_SESSION_ID, SECOND_SESSION_ID);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTokenValue).thenReturn("token");

            service.login(loginForm());
            service.login(loginForm());

            ArgumentCaptor<SaLoginParameter> parameterCaptor =
                    ArgumentCaptor.forClass(SaLoginParameter.class);
            securityUtils.verify(
                    () -> SecurityUtils.login(eq(7), parameterCaptor.capture()),
                    times(2));
            List<SaLoginParameter> parameters = parameterCaptor.getAllValues();
            assertThat(parameters)
                    .extracting(SaLoginParameter::getDeviceId)
                    .containsExactly(FIRST_SESSION_ID, SECOND_SESSION_ID);
            assertThat(parameters)
                    .extracting(SaLoginParameter::getIsShare)
                    .containsOnly(false);
            assertThat(parameters)
                    .extracting(SaLoginParameter::getTimeout)
                    .containsOnly(3_600L);
            verify(onlineSessionLifecycleService).register(user, FIRST_SESSION_ID);
            verify(onlineSessionLifecycleService).register(user, SECOND_SESSION_ID);
        }
    }

    @Test
    void delegatesActiveLogoutToTheLifecycleService() {
        service.logout();

        verify(onlineSessionLifecycleService).logoutCurrentSession();
    }

    @Test
    void failsLoginWhenOnlineSessionRegistrationFails() {
        SysUser user = SysUser.builder()
                .id(7)
                .email("user@example.com")
                .password(BCrypt.hashpw("secret", BCrypt.gensalt()))
                .status(1)
                .build();
        when(userService.getByEmail("user@example.com")).thenReturn(user);
        when(configReader.login()).thenReturn(loginConfig());
        when(onlineSessionLifecycleService.createSessionId()).thenReturn(FIRST_SESSION_ID);
        doThrow(new IllegalStateException("registry unavailable"))
                .when(onlineSessionLifecycleService).register(user, FIRST_SESSION_ID);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTokenValue).thenReturn("token");

            assertThatThrownBy(() -> service.login(loginForm()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("registry unavailable");
        }
    }

    @Test
    void rollsBackTheNewTokenWhenPostLoginInitializationFails() {
        SysUser user = SysUser.builder()
                .id(7)
                .email("user@example.com")
                .password(BCrypt.hashpw("secret", BCrypt.gensalt()))
                .status(1)
                .build();
        when(userService.getByEmail("user@example.com")).thenReturn(user);
        when(configReader.login()).thenReturn(loginConfig());
        when(onlineSessionLifecycleService.createSessionId()).thenReturn(FIRST_SESSION_ID);
        IllegalStateException failure = new IllegalStateException("token context unavailable");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTokenValue).thenThrow(failure);

            assertThatThrownBy(() -> service.login(loginForm()))
                    .isSameAs(failure);

            verify(onlineSessionLifecycleService)
                    .rollbackUnregisteredSession(7, FIRST_SESSION_ID, failure);
            verify(onlineSessionLifecycleService, never()).register(user, FIRST_SESSION_ID);
        }
    }

    private static AuthForm loginForm() {
        AuthForm form = new AuthForm();
        form.setEmail("user@example.com");
        form.setPassword("secret");
        return form;
    }

    private static LoginSettings loginConfig() {
        LoginSettings config = new LoginSettings();
        config.setMaxRetryCount(5);
        config.setLockTimeMinutes(30);
        config.setRememberMeEnabled(true);
        config.setSessionTimeoutSeconds(3_600L);
        config.setRememberMeTimeoutSeconds(259_200L);
        config.setSingleLogin(false);
        return config;
    }
}
