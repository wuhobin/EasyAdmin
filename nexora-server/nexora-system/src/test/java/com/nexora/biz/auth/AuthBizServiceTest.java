package com.nexora.biz.auth;

import com.nexora.domain.form.auth.AuthForm;
import com.nexora.config.NexoraPermissionProvider;
import com.nexora.config.SysConfigReader;
import com.nexora.cache.SecurityAuthorizationCache;
import com.nexora.constants.CommonConstants;
import com.nexora.entity.SysUser;
import com.nexora.service.SysUserService;
import com.nexora.service.SysRoleService;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.image.ImageVerificationService;
import cn.dev33.satoken.secure.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthBizServiceTest {

    @Test
    void selectsOneHourTimeoutWhenRememberMeIsFalse() {
        assertThat(AuthBizService.tokenTimeout(false)).isEqualTo(3_600L);
    }

    @Test
    void selectsThreeDayTimeoutWhenRememberMeIsTrue() {
        assertThat(AuthBizService.tokenTimeout(true)).isEqualTo(259_200L);
    }

    @Test
    void defaultsRememberMeToFalseWhenTheFieldIsMissing() throws Exception {
        AuthForm loginForm = new ObjectMapper().readValue(
                "{\"email\":\" Admin@Example.com \",\"password\":\"secret\"}",
                AuthForm.class
        );

        assertThat(loginForm.isRememberMe()).isFalse();
        assertThat(loginForm.getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void bindsRememberMeWhenTheFieldIsProvided() throws Exception {
        AuthForm loginForm = new ObjectMapper().readValue(
                "{\"email\":\"admin@example.com\",\"password\":\"secret\",\"rememberMe\":true}",
                AuthForm.class
        );

        assertThat(loginForm.isRememberMe()).isTrue();
    }

    @Test
    void storesLoginUserInfoInTheSession() {
        SysUserService userService = mock(SysUserService.class);
        SysUser user = SysUser.builder()
                .id(1)
                .email("admin@example.com")
                .nickname("Administrator")
                .password(BCrypt.hashpw("secret", BCrypt.gensalt()))
                .status(1)
                .build();
        when(userService.getByEmail("admin@example.com")).thenReturn(user);
        AuthBizService service = createService(userService, mock(NexoraPermissionProvider.class));
        AuthForm form = new AuthForm();
        form.setEmail(" Admin@Example.com ");
        form.setPassword("secret");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTokenValue).thenReturn("token");

            var loginUserInfo = service.login(form);

            securityUtils.verify(() -> SecurityUtils.setSessionAttribute(
                    CommonConstants.CURRENT_USER, loginUserInfo));
        }
    }

    @Test
    void getsRolesAndPermissionsThroughCachedPermissionProvider() {
        SysUserService userService = mock(SysUserService.class);
        NexoraPermissionProvider permissionProvider = mock(NexoraPermissionProvider.class);
        SysUser user = SysUser.builder().id(7).email("admin@example.com").build();
        when(userService.getById(7)).thenReturn(user);
        when(permissionProvider.getAuthorization(7, AccountType.LOGIN))
                .thenReturn(new SecurityAuthorizationCache.Authorization(
                        List.of("admin"), List.of("sys:config:list")));
        AuthBizService service = createService(userService, permissionProvider);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsLong).thenReturn(7L);

            var loginUserInfo = service.getLoginUserInfo();

            assertThat(loginUserInfo.getRoles()).containsExactly("admin");
            assertThat(loginUserInfo.getPermissions()).containsExactly("sys:config:list");
        }
    }

    @Test
    void publicAuthenticationEndpointsRemainInTheApplicationAllowList() throws Exception {
        String config = Files.readString(Path.of(
                "..", "nexora-boot", "src", "main", "resources", "config", "platform.yml"
        ));

        assertThat(config).contains(
                "exclude-paths:",
                "- /auth/login",
                "- /auth/register/sendCode",
                "- /auth/register",
                "- /auth/image",
                "- /auth/image/*/match",
                "- /auth/password/reset/sendCode",
                "- /auth/password/reset",
                "- /sys/config/value/**");
        assertThat(config).doesNotContain("- /auth/info", "- /auth/logout", "- /auth/verify");
    }

    @Test
    void strictSpringBeanSettingsRemainEnabled() throws Exception {
        String config = Files.readString(Path.of(
                "..", "nexora-boot", "src", "main", "resources", "application.yml"
        ));

        assertThat(config).doesNotContain(
                "allow-circular-references", "allow-bean-definition-overriding");
    }

    @SuppressWarnings("unchecked")
    private static AuthBizService createService(
            SysUserService userService, NexoraPermissionProvider permissionProvider) {
        return new AuthBizService(
                userService,
                mock(SysRoleService.class),
                permissionProvider,
                mock(SysConfigReader.class),
                mock(ObjectProvider.class),
                mock(ImageVerificationService.class));
    }
}
