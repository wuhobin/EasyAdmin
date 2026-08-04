package com.nexora.identity.biz;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.cache.LoginRetryCache;
import com.nexora.identity.security.NexoraPermissionProvider;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.system.api.SystemConfigReader;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.identity.domain.form.AuthForm;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthBizServicePasswordResetTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final MailVerificationOrchestrator mailVerificationOrchestrator = mock(MailVerificationOrchestrator.class);
    private final PasswordPolicyValidator passwordPolicyValidator = passwordPolicyValidator();
    private final AuthBizService bizService = new AuthBizService(
            userService,
            mock(NexoraPermissionProvider.class),
            mock(SystemConfigReader.class),
            new LoginSecurityService(mock(LoginRetryCache.class)),
            mock(ImageVerificationService.class),
            mock(RegistrationService.class), new PasswordResetService(userService, passwordPolicyValidator, mailVerificationOrchestrator),
            mailVerificationOrchestrator);

    @Test
    void rejectsAnEmailThatIsNotRegistered() {
        assertThatThrownBy(() -> bizService.sendResetPasswordCode(
                form("missing@example.com", null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(IdentityConstants.EMAIL_NOT_REGISTERED_MESSAGE);

        verify(mailVerificationOrchestrator, never()).sendCode(anyString(), any());
    }

    @Test
    void sendsTheResetCodeToTheNormalizedBoundEmail() {
        when(userService.getByEmail("user@example.com"))
                .thenReturn(SysUser.builder().id(7).email("user@example.com").build());

        bizService.sendResetPasswordCode(form(" User@Example.com ", null, null));

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CommonVerificationScene> sceneCaptor = ArgumentCaptor.forClass(CommonVerificationScene.class);
        verify(mailVerificationOrchestrator).sendCode(emailCaptor.capture(), sceneCaptor.capture());
        assertThat(emailCaptor.getValue()).isEqualTo("user@example.com");
        assertThat(sceneCaptor.getValue()).isEqualTo(CommonVerificationScene.RESET_PASSWORD);
    }

    @Test
    void doesNotChangeThePasswordWhenTheCodeIsInvalid() {
        SysUser user = SysUser.builder().id(7).email("user@example.com").build();
        when(userService.getByEmail("user@example.com")).thenReturn(user);
        when(mailVerificationOrchestrator.verifyCode(anyString(), any(), anyString()))
                .thenThrow(new BizException(IdentityConstants.EMAIL_CODE_INVALID_MESSAGE));

        assertThatThrownBy(() -> bizService.resetPassword(
                form("user@example.com", "123456", "new-secret")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(IdentityConstants.EMAIL_CODE_INVALID_MESSAGE);

        verify(userService, never()).updateById(any());
    }

    @Test
    void resetsThePasswordAndInvalidatesExistingSessions() {
        SysUser user = SysUser.builder().id(7).email("user@example.com").build();
        when(userService.getByEmail("user@example.com")).thenReturn(user);
        when(mailVerificationOrchestrator.verifyCode("user@example.com",
                CommonVerificationScene.RESET_PASSWORD, "123456")).thenReturn(true);
        when(userService.updateById(any())).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            bizService.resetPassword(form(" User@Example.com ", "123456", "new-secret"));

            ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
            verify(userService).updateById(captor.capture());
            SysUser update = captor.getValue();
            assertThat(update.getId()).isEqualTo(7);
            assertThat(update.getPassword()).isNotEqualTo("new-secret");
            assertThat(BCrypt.checkpw("new-secret", update.getPassword())).isTrue();
            securityUtils.verify(() -> SecurityUtils.kickout(7));
        }
    }

    private static AuthForm form(String email, String code, String password) {
        AuthForm form = new AuthForm();
        form.setEmail(email);
        form.setCode(code);
        form.setPassword(password);
        return form;
    }

    private static PasswordPolicyValidator passwordPolicyValidator() {
        PasswordPolicyValidator validator = mock(PasswordPolicyValidator.class);
        when(validator.validateNewPassword(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return validator;
    }

}
