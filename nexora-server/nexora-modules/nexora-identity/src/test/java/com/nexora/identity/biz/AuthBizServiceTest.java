package com.nexora.identity.biz;

import com.nexora.identity.domain.form.AuthForm;
import com.nexora.identity.security.NexoraPermissionProvider;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.system.api.SystemConfigReader;
import com.nexora.identity.cache.LoginRetryCache;
import com.nexora.identity.cache.SecurityAuthorizationCache;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.system.api.LoginSettings;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.service.SysUserService;
import com.nexora.identity.service.SysRoleService;
import com.aurora.starter.security.account.AccountType;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.image.ImageVerificationService;
import cn.dev33.satoken.secure.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
        assertThat(LoginSecurityService.tokenTimeout(loginConfig(), false)).isEqualTo(3_600L);
    }

    @Test
    void selectsThreeDayTimeoutWhenRememberMeIsTrue() {
        assertThat(LoginSecurityService.tokenTimeout(loginConfig(), true)).isEqualTo(259_200L);
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
                    IdentityConstants.CURRENT_USER, loginUserInfo));
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
        String config = Files.readString(repositoryRoot().resolve(Path.of(
                "nexora-server", "nexora-boot", "src", "main", "resources", "config", "platform.yml"
        )));

        assertThat(config).contains(
                "exclude-paths:",
                "- /auth/login",
                "- /auth/register/sendCode",
                "- /auth/register",
                "- /auth/image",
                "- /auth/image/*/match",
                "- /auth/password/reset/sendCode",
                "- /auth/password/reset",
                "- /sys/config-group/public");
        assertThat(config).doesNotContain("- /auth/info", "- /auth/logout", "- /auth/verify");
    }

    @Test
    void strictSpringBeanSettingsRemainEnabled() throws Exception {
        String config = Files.readString(repositoryRoot().resolve(Path.of(
                "nexora-server", "nexora-boot", "src", "main", "resources", "application.yml"
        )));

        assertThat(config).doesNotContain(
                "allow-circular-references", "allow-bean-definition-overriding");
    }

    @SuppressWarnings("unchecked")
    private static AuthBizService createService(
            SysUserService userService, NexoraPermissionProvider permissionProvider) {
        SystemConfigReader configReader = mock(SystemConfigReader.class);
        when(configReader.login()).thenReturn(loginConfig());
        return new AuthBizService(
                userService,
                permissionProvider,
                configReader,
                new LoginSecurityService(mock(LoginRetryCache.class)),
                mock(ImageVerificationService.class),
                mock(RegistrationService.class), mock(PasswordResetService.class),
                mock(MailVerificationOrchestrator.class));
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

    private static Path repositoryRoot() {
        Path currentPath = Path.of("").toAbsolutePath().normalize();
        while (currentPath != null) {
            if (Files.isRegularFile(currentPath.resolve("nexora-admin.sql"))
                    && Files.isDirectory(currentPath.resolve("deploy"))) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }
        throw new IllegalStateException("Unable to locate the repository root");
    }
}
