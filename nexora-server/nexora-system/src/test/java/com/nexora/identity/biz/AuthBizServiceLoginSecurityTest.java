package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.cache.LoginRetryCache;
import com.nexora.identity.security.NexoraPermissionProvider;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.config.SysConfigGroupReader;
import com.nexora.constants.CommonConstants;
import com.nexora.identity.domain.form.AuthForm;
import com.nexora.domain.form.system.config.LoginConfigForm;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthBizServiceLoginSecurityTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final SysConfigGroupReader configReader = mock(SysConfigGroupReader.class);
    private final LoginRetryCache loginRetryCache = mock(LoginRetryCache.class);
    private final ImageVerificationService imageVerificationService = mock(ImageVerificationService.class);
    private final AuthBizService bizService = createService();

    @Test
    void verifiesTheSliderBeforeLookingUpTheAccount() {
        LoginConfigForm config = loginConfig();
        config.setCaptchaEnabled(true);
        when(configReader.login()).thenReturn(config);
        when(imageVerificationService.verifyAndConsume("captcha-id")).thenReturn(false);

        assertThatThrownBy(() -> bizService.login(loginForm("secret", "captcha-id")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.IMAGE_CAPTCHA_INVALID_MESSAGE);

        verify(userService, never()).getByEmail(any());
        verify(loginRetryCache, never()).getFailureCount(any());
    }

    @Test
    void rejectsALockedAccountBeforePasswordVerification() {
        when(configReader.login()).thenReturn(loginConfig());
        when(loginRetryCache.getFailureCount("user@example.com")).thenReturn(5);
        when(loginRetryCache.getRemainingMinutes("user@example.com")).thenReturn(12L);

        assertThatThrownBy(() -> bizService.login(loginForm("secret", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("12分钟后重试");

        verify(userService, never()).getByEmail(any());
    }

    @Test
    void locksAtTheConfiguredFailureThreshold() {
        when(configReader.login()).thenReturn(loginConfig());
        when(loginRetryCache.getFailureCount("user@example.com")).thenReturn(4);
        when(loginRetryCache.recordFailure("user@example.com", 30)).thenReturn(5);
        when(loginRetryCache.getRemainingMinutes("user@example.com")).thenReturn(30L);

        assertThatThrownBy(() -> bizService.login(loginForm("wrong", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("30分钟后重试");

        verify(loginRetryCache).recordFailure("user@example.com", 30);
    }

    @Test
    void refusesLoginWhenTheRetryStoreIsUnavailable() {
        when(configReader.login()).thenReturn(loginConfig());
        when(loginRetryCache.getFailureCount("user@example.com"))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> bizService.login(loginForm("secret", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.LOGIN_SECURITY_UNAVAILABLE_MESSAGE);

        verify(userService, never()).getByEmail(any());
    }

    @Test
    void doesNotCreateASessionForAPendingUser() {
        when(configReader.login()).thenReturn(loginConfig());
        when(userService.getByEmail("user@example.com")).thenReturn(user(2));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            assertThatThrownBy(() -> bizService.login(loginForm("secret", null)))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining(CommonConstants.ACCOUNT_PENDING_MESSAGE);

            securityUtils.verifyNoInteractions();
        }
    }

    @Test
    void kicksOutExistingSessionsWhenSingleLoginIsEnabled() {
        LoginConfigForm config = loginConfig();
        config.setSingleLogin(true);
        when(configReader.login()).thenReturn(config);
        when(userService.getByEmail("user@example.com")).thenReturn(user(1));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTokenValue).thenReturn("token");

            bizService.login(loginForm("secret", null));

            securityUtils.verify(() -> SecurityUtils.kickout(7));
        }
    }

    @SuppressWarnings("unchecked")
    private AuthBizService createService() {
        return new AuthBizService(
                userService,
                mock(SysRoleService.class),
                mock(NexoraPermissionProvider.class),
                configReader,
                mock(PasswordPolicyValidator.class),
                new LoginSecurityService(loginRetryCache),
                mock(ObjectProvider.class),
                imageVerificationService);
    }

    private static LoginConfigForm loginConfig() {
        LoginConfigForm config = new LoginConfigForm();
        config.setCaptchaEnabled(false);
        config.setMaxRetryCount(5);
        config.setLockTimeMinutes(30);
        config.setRememberMeEnabled(true);
        config.setSessionTimeoutSeconds(3_600L);
        config.setRememberMeTimeoutSeconds(259_200L);
        config.setSingleLogin(false);
        return config;
    }

    private static AuthForm loginForm(String password, String captchaId) {
        AuthForm form = new AuthForm();
        form.setEmail(" User@Example.com ");
        form.setPassword(password);
        form.setCaptchaId(captchaId);
        return form;
    }

    private static SysUser user(int status) {
        return SysUser.builder()
                .id(7)
                .email("user@example.com")
                .password(BCrypt.hashpw("secret", BCrypt.gensalt()))
                .status(status)
                .build();
    }
}
