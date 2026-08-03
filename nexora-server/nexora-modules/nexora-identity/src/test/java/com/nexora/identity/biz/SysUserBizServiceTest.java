package com.nexora.identity.biz;

import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.nexora.constants.CommonConstants;
import com.nexora.identity.constants.SysUserStatusEnum;
import com.nexora.identity.config.PasswordPolicyValidator;
import com.nexora.identity.domain.form.SysUserForm;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.cache.SecurityAuthorizationCache;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import com.nexora.contract.UserDeletionCleanup;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserBizServiceTest {

    @Test
    void updatesOnlyTheCurrentUsersProfile() {
        SysUserService userService = mock(SysUserService.class);
        SysUserBizService service = new SysUserBizService(userService, mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class), mailProvider(null),
                List.of(), mock(PasswordPolicyValidator.class));
        SysUserForm form = new SysUserForm();
        form.setNickname("new-name");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            service.updateProfile(form);
        }

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7);
        assertThat(captor.getValue().getNickname()).isEqualTo("new-name");
        assertThat(captor.getValue().getEmail()).isNull();
        assertThat(captor.getValue().getPassword()).isNull();
        assertThat(captor.getValue().getStatus()).isNull();
    }

    @Test
    void deletesMailAccountsAndEvictsAuthorizationWhenUsersAreDeleted() {
        SysUserService userService = mock(SysUserService.class);
        SysRoleService roleService = mock(SysRoleService.class);
        SecurityAuthorizationCache authorizationCache = mock(SecurityAuthorizationCache.class);
        UserDeletionCleanup cleanup = mock(UserDeletionCleanup.class);
        SysUserBizService service = new SysUserBizService(
                userService, roleService, authorizationCache, mailProvider(null), List.of(cleanup),
                mock(PasswordPolicyValidator.class));

        service.delete(List.of(7, 8));

        verify(cleanup).cleanup(List.of(7, 8));
        verify(authorizationCache).evictUsersAfterCommit(List.of(7, 8));
    }

    @Test
    void changesOnlyTheCurrentUsersEmailAfterTheCodeIsConsumed() {
        SysUserService userService = mock(SysUserService.class);
        MailVerificationService verificationService = mock(MailVerificationService.class);
        SysUser currentUser = SysUser.builder()
                .id(7)
                .email("old@example.com")
                .build();
        when(userService.getById(7)).thenReturn(currentUser);
        when(userService.getByEmail("new@example.com")).thenReturn(null);
        when(verificationService.verifyAndConsume(
                new MailVerificationVerifyRequest(
                        "new@example.com",
                        com.aurora.starter.verification.scene.CommonVerificationScene.CHANGE_EMAIL,
                        "123456")))
                .thenReturn(true);
        SysUserBizService service = new SysUserBizService(
                userService,
                mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class),
                mailProvider(verificationService),
                List.of(), mock(PasswordPolicyValidator.class));
        SysUserForm form = new SysUserForm();
        form.setEmail(" New@Example.com ");
        form.setCode("123456");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);
            service.changeEmail(form);
        }

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7);
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getPassword()).isNull();
    }

    @Test
    void refusesToDeleteTheRootUser() {
        SysUserBizService service = new SysUserBizService(
                mock(SysUserService.class),
                mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class),
                mailProvider(null),
                List.of(), mock(PasswordPolicyValidator.class));

        assertThatThrownBy(() -> service.delete(List.of(1, 7)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("根用户不能删除");
    }

    @Test
    void refusesToDisableTheRootUser() {
        SysUserService userService = mock(SysUserService.class);
        when(userService.getById(1)).thenReturn(SysUser.builder().id(1).build());
        SysUserBizService service = new SysUserBizService(
                userService,
                mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class),
                mailProvider(null),
                List.of(), mock(PasswordPolicyValidator.class));
        SysUserForm form = new SysUserForm();
        form.setId(1);
        form.setNickname("Root");
        form.setStatus(0);
        form.setRoleIds(List.of(1));

        assertThatThrownBy(() -> service.update(form))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("根用户不能停用");
    }

    @Test
    void rejectsAnOccupiedEmailBeforeSendingTheCode() {
        SysUserService userService = mock(SysUserService.class);
        when(userService.getById(7)).thenReturn(SysUser.builder()
                .id(7).email("old@example.com").build());
        when(userService.getByEmail("used@example.com")).thenReturn(SysUser.builder()
                .id(8).email("used@example.com").build());
        SysUserBizService service = new SysUserBizService(
                userService,
                mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class),
                mailProvider(null),
                List.of(), mock(PasswordPolicyValidator.class));
        SysUserForm form = new SysUserForm();
        form.setEmail(" Used@Example.com ");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);
            assertThatThrownBy(() -> service.sendEmailCode(form))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("邮箱已经被使用");
        }
    }

    @Test
    void sendsChangeEmailCodeAsRichHtml() {
        SysUserService userService = mock(SysUserService.class);
        MailVerificationService verificationService = mock(MailVerificationService.class);
        when(userService.getById(7)).thenReturn(SysUser.builder()
                .id(7).email("old@example.com").build());
        when(userService.getByEmail("new@example.com")).thenReturn(null);
        SysUserBizService service = new SysUserBizService(
                userService,
                mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class),
                mailProvider(verificationService),
                List.of(), mock(PasswordPolicyValidator.class));
        SysUserForm form = new SysUserForm();
        form.setEmail("new@example.com");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);
            service.sendEmailCode(form);
        }

        ArgumentCaptor<MailVerificationSendRequest> captor =
                ArgumentCaptor.forClass(MailVerificationSendRequest.class);
        verify(verificationService).send(captor.capture());
        MailVerificationSendRequest request = captor.getValue();
        assertThat(request.email()).isEqualTo("new@example.com");
        assertThat(request.subject()).isEqualTo(CommonConstants.CHANGE_EMAIL_SUBJECT);
        assertThat(request.contentType()).isEqualTo(MailContentType.HTML);
        assertThat(request.content())
                .contains("<!doctype html>", "{code}", "{expireMinutes}", "邮箱换绑验证", "安全提示");
    }

    @Test
    void administratorUpdateIgnoresEmailAndPasswordFields() {
        SysUserService userService = mock(SysUserService.class);
        SysRoleService roleService = mock(SysRoleService.class);
        when(userService.getById(7)).thenReturn(SysUser.builder().id(7).build());
        SysUserBizService service = new SysUserBizService(
                userService,
                roleService,
                mock(SecurityAuthorizationCache.class),
                mailProvider(null),
                List.of(), mock(PasswordPolicyValidator.class));
        SysUserForm form = new SysUserForm();
        form.setId(7);
        form.setNickname("new-name");
        form.setEmail("should-be-ignored@example.com");
        form.setPassword("ignored-password");
        form.setRoleIds(List.of(2));

        service.update(form);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7);
        assertThat(captor.getValue().getNickname()).isEqualTo("new-name");
        assertThat(captor.getValue().getEmail()).isNull();
        assertThat(captor.getValue().getPassword()).isNull();
    }

    @Test
    void refusesToAddAUserWithAnUnsupportedStatus() {
        SysUserService userService = mock(SysUserService.class);
        SysUserBizService service = new SysUserBizService(
                userService,
                mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class),
                mailProvider(null),
                List.of(),
                mock(PasswordPolicyValidator.class));
        SysUserForm form = new SysUserForm();
        form.setStatus(3);

        assertThatThrownBy(() -> service.add(form))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.USER_STATUS_INVALID_MESSAGE);
        verify(userService, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesToUpdateAUserWithAnUnsupportedStatus() {
        SysUserService userService = mock(SysUserService.class);
        SysUserBizService service = new SysUserBizService(
                userService,
                mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class),
                mailProvider(null),
                List.of(),
                mock(PasswordPolicyValidator.class));
        SysUserForm form = new SysUserForm();
        form.setId(7);
        form.setStatus(-1);

        assertThatThrownBy(() -> service.update(form))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.USER_STATUS_INVALID_MESSAGE);
        verify(userService, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void auditsAPendingUserAndEvictsItsAuthorization() {
        SysUserService userService = mock(SysUserService.class);
        SecurityAuthorizationCache authorizationCache = mock(SecurityAuthorizationCache.class);
        when(userService.getById(7)).thenReturn(SysUser.builder()
                .id(7).status(SysUserStatusEnum.PENDING.getCode()).build());
        when(userService.updateById(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        SysUserBizService service = new SysUserBizService(
                userService,
                mock(SysRoleService.class),
                authorizationCache,
                mailProvider(null),
                List.of(),
                mock(PasswordPolicyValidator.class));

        service.audit(7);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7);
        assertThat(captor.getValue().getStatus()).isEqualTo(SysUserStatusEnum.NORMAL.getCode());
        verify(authorizationCache).evictUsersAfterCommit(List.of(7));
    }

    @Test
    void refusesToAuditAUserThatIsNotPending() {
        SysUserService userService = mock(SysUserService.class);
        when(userService.getById(7)).thenReturn(SysUser.builder()
                .id(7).status(SysUserStatusEnum.NORMAL.getCode()).build());
        SysUserBizService service = new SysUserBizService(
                userService,
                mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class),
                mailProvider(null),
                List.of(),
                mock(PasswordPolicyValidator.class));

        assertThatThrownBy(() -> service.audit(7))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.USER_NOT_PENDING_MESSAGE);

        verify(userService, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MailVerificationService> mailProvider(
            MailVerificationService verificationService) {
        ObjectProvider<MailVerificationService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(verificationService);
        return provider;
    }
}
