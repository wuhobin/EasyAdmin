package com.nexora.biz.auth;

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
import com.nexora.entity.SysRole;
import com.nexora.entity.SysUser;
import com.nexora.service.SysRoleService;
import com.nexora.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthBizServiceRegistrationTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final SysRoleService roleService = mock(SysRoleService.class);
    private final SysConfigReader configReader = mock(SysConfigReader.class);
    private final MailVerificationService verificationService = mock(MailVerificationService.class);
    private final AuthBizService bizService = new AuthBizService(
            userService,
            roleService,
            mock(NexoraPermissionProvider.class),
            configReader,
            mailProvider(verificationService));

    @Test
    void onlyTheExactTrueValueEnablesRegistration() {
        when(configReader.getString(CommonConstants.REGISTER_ENABLED_CONFIG_KEY, null))
                .thenReturn("TRUE");

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
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(42);
            return true;
        }).when(userService).save(any(SysUser.class));

        bizService.register(form(
                " ABCDEFGHIJKLMNOPQRSTUVWXYZ123456@Example.com ", "123456", "secret"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).save(captor.capture());
        SysUser user = captor.getValue();
        assertThat(user.getEmail()).isEqualTo("abcdefghijklmnopqrstuvwxyz123456@example.com");
        assertThat(user.getNickname()).isEqualTo("abcdefghijklmnopqrstuvwxyz1234");
        assertThat(user.getStatus()).isEqualTo(CommonConstants.YES);
        assertThat(user.getPassword()).isNotEqualTo("secret");
        verify(roleService).addUserRoles(42, List.of(9));
    }

    @Test
    void doesNotCreateAUserWhenTheRegistrationCodeIsInvalid() {
        SysRole role = new SysRole();
        role.setId(9);
        enableRegistration(role);
        when(verificationService.verifyAndConsume(any())).thenReturn(false);

        assertThatThrownBy(() -> bizService.register(
                form("user@example.com", "123456", "secret")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.EMAIL_CODE_INVALID_MESSAGE);
        verify(userService, never()).save(any());
        verify(roleService, never()).addUserRoles(any(), any());
    }

    private void enableRegistration(SysRole role) {
        when(configReader.getString(CommonConstants.REGISTER_ENABLED_CONFIG_KEY, null))
                .thenReturn(CommonConstants.TRUE_VALUE);
        when(configReader.getString(CommonConstants.REGISTER_ROLE_CODE_CONFIG_KEY, null))
                .thenReturn("user");
        when(roleService.getByCode("user")).thenReturn(role);
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
