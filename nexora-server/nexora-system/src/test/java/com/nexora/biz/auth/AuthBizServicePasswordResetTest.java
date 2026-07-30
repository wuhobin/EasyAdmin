package com.nexora.biz.auth;

import cn.dev33.satoken.secure.BCrypt;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.config.NexoraPermissionProvider;
import com.nexora.config.SysConfigReader;
import com.nexora.constants.CommonConstants;
import com.nexora.domain.form.auth.AuthForm;
import com.nexora.entity.SysUser;
import com.nexora.service.SysRoleService;
import com.nexora.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthBizServicePasswordResetTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final MailVerificationService verificationService = mock(MailVerificationService.class);
    private final AuthBizService bizService = new AuthBizService(
            userService,
            mock(SysRoleService.class),
            mock(NexoraPermissionProvider.class),
            mock(SysConfigReader.class),
            mailProvider(verificationService),
            mock(ImageVerificationService.class));

    @Test
    void rejectsAnEmailThatIsNotRegistered() {
        assertThatThrownBy(() -> bizService.sendResetPasswordCode(
                form("missing@example.com", null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.EMAIL_NOT_REGISTERED_MESSAGE);

        verify(verificationService, never()).send(any());
    }

    @Test
    void sendsTheResetCodeToTheNormalizedBoundEmail() {
        when(userService.getByEmail("user@example.com"))
                .thenReturn(SysUser.builder().id(7).email("user@example.com").build());

        bizService.sendResetPasswordCode(form(" User@Example.com ", null, null));

        ArgumentCaptor<MailVerificationSendRequest> captor =
                ArgumentCaptor.forClass(MailVerificationSendRequest.class);
        verify(verificationService).send(captor.capture());
        MailVerificationSendRequest request = captor.getValue();
        assertThat(request.email()).isEqualTo("user@example.com");
        assertThat(request.scene()).isEqualTo(CommonVerificationScene.RESET_PASSWORD);
        assertThat(request.subject()).isEqualTo(CommonConstants.RESET_PASSWORD_EMAIL_SUBJECT);
        assertThat(request.contentType()).isEqualTo(MailContentType.HTML);
        assertThat(request.content()).contains("忘记密码验证", "忘记密码", "{code}");
    }

    @Test
    void doesNotChangeThePasswordWhenTheCodeIsInvalid() {
        SysUser user = SysUser.builder().id(7).email("user@example.com").build();
        when(userService.getByEmail("user@example.com")).thenReturn(user);
        when(verificationService.verifyAndConsume(any())).thenReturn(false);

        assertThatThrownBy(() -> bizService.resetPassword(
                form("user@example.com", "123456", "new-secret")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.EMAIL_CODE_INVALID_MESSAGE);

        verify(userService, never()).updateById(any());
    }

    @Test
    void resetsThePasswordAndInvalidatesExistingSessions() {
        SysUser user = SysUser.builder().id(7).email("user@example.com").build();
        when(userService.getByEmail("user@example.com")).thenReturn(user);
        when(verificationService.verifyAndConsume(new MailVerificationVerifyRequest(
                "user@example.com", CommonVerificationScene.RESET_PASSWORD, "123456")))
                .thenReturn(true);
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

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MailVerificationService> mailProvider(
            MailVerificationService verificationService) {
        ObjectProvider<MailVerificationService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(verificationService);
        return provider;
    }
}
