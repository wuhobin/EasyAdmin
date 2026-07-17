package com.aurora.biz;

import com.aurora.domain.form.system.UserProfileForm;
import com.aurora.entity.SysUser;
import com.aurora.service.SysRoleService;
import com.aurora.service.SysUserService;
import com.aurora.starter.security.context.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class SysUserBizServiceTest {

    @Test
    void updatesOnlyTheCurrentUsersProfile() {
        SysUserService userService = mock(SysUserService.class);
        SysUserBizService service = new SysUserBizService(userService, mock(SysRoleService.class));
        UserProfileForm form = new UserProfileForm();
        form.setNickname("new-name");
        form.setEmail("new@example.com");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(7);

            service.updateProfile(form);
        }

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7);
        assertThat(captor.getValue().getNickname()).isEqualTo("new-name");
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getPassword()).isNull();
        assertThat(captor.getValue().getStatus()).isNull();
    }
}
