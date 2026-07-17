package com.aurora.biz;

import com.aurora.domain.form.auth.LoginForm;
import com.aurora.common.Constants;
import com.aurora.entity.SysUser;
import com.aurora.service.SysMenuService;
import com.aurora.service.SysRoleService;
import com.aurora.service.SysUserService;
import com.aurora.starter.security.context.SecurityUtils;
import cn.dev33.satoken.secure.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;

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
        LoginForm loginForm = new ObjectMapper().readValue(
                "{\"username\":\"admin\",\"password\":\"secret\"}",
                LoginForm.class
        );

        assertThat(loginForm.isRememberMe()).isFalse();
    }

    @Test
    void bindsRememberMeWhenTheFieldIsProvided() throws Exception {
        LoginForm loginForm = new ObjectMapper().readValue(
                "{\"username\":\"admin\",\"password\":\"secret\",\"rememberMe\":true}",
                LoginForm.class
        );

        assertThat(loginForm.isRememberMe()).isTrue();
    }

    @Test
    void storesLoginUserInfoInTheSession() {
        SysUserService userService = mock(SysUserService.class);
        SysUser user = SysUser.builder()
                .id(1)
                .username("admin")
                .nickname("Administrator")
                .password(BCrypt.hashpw("secret", BCrypt.gensalt()))
                .status(1)
                .build();
        when(userService.getByUsername("admin")).thenReturn(user);
        AuthBizService service = new AuthBizService(
                userService, mock(SysRoleService.class), mock(SysMenuService.class));
        LoginForm form = new LoginForm();
        form.setUsername("admin");
        form.setPassword("secret");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getTokenValue).thenReturn("token");

            var loginUserInfo = service.login(form);

            securityUtils.verify(() -> SecurityUtils.setSessionAttribute(
                    Constants.CURRENT_USER, loginUserInfo));
        }
    }

    @Test
    void onlyLoginRemainsInTheApplicationAuthenticationAllowList() throws Exception {
        String config = Files.readString(Path.of(
                "..", "aurora-server", "src", "main", "resources", "config", "platform.yml"
        ));

        assertThat(config).contains("exclude-paths:", "- /auth/login");
        assertThat(config).doesNotContain("- /auth/info", "- /auth/logout", "- /auth/verify");
    }

    @Test
    void strictSpringBeanSettingsRemainEnabled() throws Exception {
        String config = Files.readString(Path.of(
                "..", "aurora-server", "src", "main", "resources", "application.yml"
        ));

        assertThat(config).doesNotContain(
                "allow-circular-references", "allow-bean-definition-overriding");
    }
}
