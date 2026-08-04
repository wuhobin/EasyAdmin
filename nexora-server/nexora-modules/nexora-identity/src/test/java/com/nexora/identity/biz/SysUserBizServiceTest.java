package com.nexora.identity.biz;

import com.nexora.identity.domain.form.SysUserForm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies that SysUserBizService correctly delegates to UserManagementService and UserProfileService.
 * Detailed behavior tests live in UserManagementServiceTest / UserProfileServiceTest.
 */
class SysUserBizServiceTest {

    private final UserManagementService userManagementService = mock(UserManagementService.class);
    private final UserProfileService userProfileService = mock(UserProfileService.class);
    private final SysUserBizService service = new SysUserBizService(userManagementService, userProfileService);

    @Test
    void delegatesAddToUserManagementService() {
        SysUserForm form = new SysUserForm();
        service.add(form);
        verify(userManagementService).add(form);
    }

    @Test
    void delegatesUpdateToUserManagementService() {
        SysUserForm form = new SysUserForm();
        service.update(form);
        verify(userManagementService).update(form);
    }

    @Test
    void delegatesDeleteToUserManagementService() {
        service.delete(List.of(7, 8));
        verify(userManagementService).delete(List.of(7, 8));
    }

    @Test
    void delegatesAuditToUserManagementService() {
        service.audit(7);
        verify(userManagementService).audit(7);
    }

    @Test
    void delegatesUpdatePasswordToUserProfileService() {
        SysUserForm form = new SysUserForm();
        service.updatePassword(form);
        verify(userProfileService).updatePassword(form);
    }

    @Test
    void delegatesProfileToUserProfileService() {
        service.profile();
        verify(userProfileService).profile();
    }

    @Test
    void delegatesSendEmailCodeToUserProfileService() {
        SysUserForm form = new SysUserForm();
        service.sendEmailCode(form);
        verify(userProfileService).sendEmailCode(form);
    }

    @Test
    void delegatesChangeEmailToUserProfileService() {
        SysUserForm form = new SysUserForm();
        service.changeEmail(form);
        verify(userProfileService).changeEmail(form);
    }

    @Test
    void delegatesVerifyPasswordToUserProfileService() {
        service.verifyPassword("secret");
        verify(userProfileService).verifyPassword("secret");
    }
}
