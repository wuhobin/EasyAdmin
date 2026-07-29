package com.nexora.biz.system;

import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.nexora.constants.CommonConstants;
import com.nexora.domain.form.system.SysUserForm;
import com.nexora.entity.SysUser;
import com.nexora.cache.SecurityAuthorizationCache;
import com.nexora.service.SysRoleService;
import com.nexora.service.SysUserService;
import com.nexora.service.MailAccountService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserBizServiceTest {

    @Test
    void updatesOnlyTheCurrentUsersProfile() {
        SysUserService userService = mock(SysUserService.class);
        SysUserBizService service = new SysUserBizService(userService, mock(SysRoleService.class),
                mock(SecurityAuthorizationCache.class), mailProvider(null),
                mock(MailAccountService.class));
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
        MailAccountService mailAccountService = mock(MailAccountService.class);
        SysUserBizService service = new SysUserBizService(
                userService, roleService, authorizationCache, mailProvider(null), mailAccountService);

        service.delete(List.of(7, 8));

        verify(mailAccountService).removeByOwnerIds(List.of(7, 8));
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
                mock(MailAccountService.class));
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
                mock(MailAccountService.class));

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
                mock(MailAccountService.class));
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
                mock(MailAccountService.class));
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
                mock(MailAccountService.class));
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
                mock(MailAccountService.class));
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

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MailVerificationService> mailProvider(
            MailVerificationService verificationService) {
        ObjectProvider<MailVerificationService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(verificationService);
        return provider;
    }
}
