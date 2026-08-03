package com.nexora.identity.biz;

import com.aurora.starter.verification.exception.ImageVerificationException;
import com.aurora.starter.verification.image.ImageVerificationService;
import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.cache.LoginRetryCache;
import com.nexora.identity.security.NexoraPermissionProvider;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.system.config.SysConfigGroupReader;
import com.nexora.constants.CommonConstants;
import com.nexora.identity.constants.SysUserStatusEnum;
import com.nexora.identity.domain.form.AuthForm;
import com.nexora.system.domain.form.RegisterConfigForm;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthBizServiceRegistrationTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final SysRoleService roleService = mock(SysRoleService.class);
    private final SysConfigGroupReader configReader = mock(SysConfigGroupReader.class);
    private final PasswordPolicyValidator passwordPolicyValidator = mock(PasswordPolicyValidator.class);
    private final MailVerificationService verificationService = mock(MailVerificationService.class);
    private final ImageVerificationService imageVerificationService = mock(ImageVerificationService.class);
    private final AuthBizService bizService = new AuthBizService(
            userService,
            roleService,
            mock(NexoraPermissionProvider.class),
            configReader,
            passwordPolicyValidator,
            new LoginSecurityService(mock(LoginRetryCache.class)),
            mailProvider(verificationService),
            imageVerificationService);

    @Test
    void disabledRegistrationIsRejected() {
        RegisterConfigForm config = registerConfig();
        config.setEnabled(false);
        when(configReader.register()).thenReturn(config);

        assertThatThrownBy(() -> bizService.sendRegisterCode(form("user@example.com", null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.REGISTER_DISABLED_MESSAGE);
        verify(verificationService, never()).send(any());
    }

    @Test
    void rejectsRegistrationWhenTheConfiguredRoleDoesNotExist() {
        enableRegistration(null);

        assertThatThrownBy(() -> bizService.sendRegisterCode(form("user@example.com", null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.REGISTER_CONFIG_INCOMPLETE_MESSAGE);
        verify(verificationService, never()).send(any());
    }

    @Test
    void rejectsAnOccupiedEmailBeforeSendingTheRegistrationCode() {
        enableRegistration(new SysRole());
        when(userService.getByEmail("used@example.com"))
                .thenReturn(SysUser.builder().id(7).email("used@example.com").build());

        assertThatThrownBy(() -> bizService.sendRegisterCode(form(" Used@Example.com ", null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.EMAIL_IN_USE_MESSAGE);
        verify(verificationService, never()).send(any());
    }

    @Test
    void sendsTheRegistrationCodeAsRichHtml() {
        enableRegistration(new SysRole());

        bizService.sendRegisterCode(form(" User@Example.com ", null, null));

        ArgumentCaptor<MailVerificationSendRequest> captor =
                ArgumentCaptor.forClass(MailVerificationSendRequest.class);
        verify(verificationService).send(captor.capture());
        MailVerificationSendRequest request = captor.getValue();
        assertThat(request.email()).isEqualTo("user@example.com");
        assertThat(request.scene()).isEqualTo(CommonVerificationScene.REGISTER);
        assertThat(request.subject()).isEqualTo(CommonConstants.REGISTER_EMAIL_SUBJECT);
        assertThat(request.contentType()).isEqualTo(MailContentType.HTML);
        assertThat(request.content()).contains("注册验证", "账号注册", "{code}");
    }

    @Test
    void createsAnEnabledUserWithGeneratedNicknameAndConfiguredRole() {
        SysRole role = new SysRole();
        role.setId(9);
        role.setCode("user");
        enableRegistration(role);
        when(verificationService.verifyAndConsume(new MailVerificationVerifyRequest(
                "abcdefghijklmnopqrstuvwxyz123456@example.com",
                CommonVerificationScene.REGISTER,
                "123456"))).thenReturn(true);
        when(imageVerificationService.verifyAndConsume("captcha-id")).thenReturn(true);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(42);
            return true;
        }).when(userService).save(any(SysUser.class));

        bizService.register(registrationForm(
                " ABCDEFGHIJKLMNOPQRSTUVWXYZ123456@Example.com ",
                "123456",
                "secret",
                "captcha-id"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).save(captor.capture());
        SysUser user = captor.getValue();
        assertThat(user.getEmail()).isEqualTo("abcdefghijklmnopqrstuvwxyz123456@example.com");
        assertThat(user.getNickname()).isEqualTo("abcdefghijklmnopqrstuvwxyz1234");
        assertThat(user.getStatus()).isEqualTo(CommonConstants.YES);
        assertThat(user.getPassword()).isNotEqualTo("secret");
        InOrder verificationOrder = inOrder(imageVerificationService, verificationService);
        verificationOrder.verify(imageVerificationService).verifyAndConsume("captcha-id");
        verificationOrder.verify(verificationService).verifyAndConsume(new MailVerificationVerifyRequest(
                "abcdefghijklmnopqrstuvwxyz123456@example.com",
                CommonVerificationScene.REGISTER,
                "123456"));
        verify(roleService).addUserRoles(42, List.of(9));
    }

    @Test
    void createsAPendingUserWhenRegistrationAuditIsEnabled() {
        SysRole role = new SysRole();
        role.setId(9);
        RegisterConfigForm config = registerConfig();
        config.setVerifyEmail(false);
        config.setNeedAudit(true);
        when(configReader.register()).thenReturn(config);
        when(passwordPolicyValidator.validateNewPassword(any())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(roleService.getByCode("user")).thenReturn(role);
        when(imageVerificationService.verifyAndConsume("captcha-id")).thenReturn(true);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(42);
            return true;
        }).when(userService).save(any(SysUser.class));

        bizService.register(registrationForm(
                "user@example.com", null, "secret", "captcha-id"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SysUserStatusEnum.PENDING.getCode());
        verify(verificationService, never()).verifyAndConsume(any());
    }

    @Test
    void rejectsRegistrationWhenTheImageCaptchaIdIsMissing() {
        SysRole role = new SysRole();
        role.setId(9);
        enableRegistration(role);

        assertThatThrownBy(() -> bizService.register(
                form("user@example.com", "123456", "secret")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.IMAGE_CAPTCHA_REQUIRED_MESSAGE);

        verify(imageVerificationService, never()).verifyAndConsume(any());
        verify(verificationService, never()).verifyAndConsume(any());
        verify(userService, never()).save(any());
    }

    @Test
    void doesNotCreateAUserWhenTheImageCaptchaIsInvalid() {
        SysRole role = new SysRole();
        role.setId(9);
        enableRegistration(role);
        when(imageVerificationService.verifyAndConsume("captcha-id")).thenReturn(false);

        assertThatThrownBy(() -> bizService.register(registrationForm(
                "user@example.com", "123456", "secret", "captcha-id")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.IMAGE_CAPTCHA_INVALID_MESSAGE);

        verify(verificationService, never()).verifyAndConsume(any());
        verify(userService, never()).save(any());
        verify(roleService, never()).addUserRoles(any(), any());
    }

    @Test
    void doesNotCreateAUserWhenImageCaptchaVerificationFails() {
        SysRole role = new SysRole();
        role.setId(9);
        enableRegistration(role);
        when(imageVerificationService.verifyAndConsume("captcha-id"))
                .thenThrow(new ImageVerificationException("redis unavailable"));

        assertThatThrownBy(() -> bizService.register(registrationForm(
                "user@example.com", "123456", "secret", "captcha-id")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.IMAGE_CAPTCHA_VERIFY_FAILED_MESSAGE);

        verify(verificationService, never()).verifyAndConsume(any());
        verify(userService, never()).save(any());
        verify(roleService, never()).addUserRoles(any(), any());
    }

    @Test
    void doesNotCreateAUserWhenTheRegistrationCodeIsInvalid() {
        SysRole role = new SysRole();
        role.setId(9);
        enableRegistration(role);
        when(imageVerificationService.verifyAndConsume("captcha-id")).thenReturn(true);
        when(verificationService.verifyAndConsume(any())).thenReturn(false);

        assertThatThrownBy(() -> bizService.register(
                registrationForm("user@example.com", "123456", "secret", "captcha-id")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.EMAIL_CODE_INVALID_MESSAGE);
        verify(userService, never()).save(any());
        verify(roleService, never()).addUserRoles(any(), any());
    }

    private void enableRegistration(SysRole role) {
        when(configReader.register()).thenReturn(registerConfig());
        when(passwordPolicyValidator.validateNewPassword(any())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(roleService.getByCode("user")).thenReturn(role);
    }

    private static RegisterConfigForm registerConfig() {
        RegisterConfigForm config = new RegisterConfigForm();
        config.setEnabled(true);
        config.setVerifyEmail(true);
        config.setDefaultRoleCode("user");
        config.setNeedAudit(false);
        return config;
    }

    private static AuthForm form(String email, String code, String password) {
        AuthForm form = new AuthForm();
        form.setEmail(email);
        form.setCode(code);
        form.setPassword(password);
        return form;
    }

    private static AuthForm registrationForm(
            String email, String code, String password, String captchaId) {
        AuthForm form = form(email, code, password);
        form.setCaptchaId(captchaId);
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
