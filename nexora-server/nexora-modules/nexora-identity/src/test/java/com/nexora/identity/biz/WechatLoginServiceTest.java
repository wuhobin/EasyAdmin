package com.nexora.identity.biz;

import com.aurora.starter.redis.core.RedisCache;
import com.nexora.identity.constants.LoginTypeEnum;
import com.nexora.identity.constants.SysUserStatusEnum;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.entity.UserIdentity;
import com.nexora.identity.infrastructure.WechatMpClientManager;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import com.nexora.identity.service.UserIdentityService;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemConfigReader;
import com.nexora.system.api.WechatLoginSettings;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatLoginServiceTest {

    @Test
    void createsASeparatePendingUserWithTheDefaultRegistrationRole() {
        SystemConfigReader configReader = mock(SystemConfigReader.class);
        WechatMpClientManager clientManager = mock(WechatMpClientManager.class);
        UserIdentityService identityService = mock(UserIdentityService.class);
        SysUserService userService = mock(SysUserService.class);
        SysRoleService roleService = mock(SysRoleService.class);
        RegistrationSettings registration = new RegistrationSettings();
        registration.setDefaultRoleCode("user");
        registration.setNeedAudit(true);
        WechatLoginSettings wechat = new WechatLoginSettings();
        wechat.setAppId("wx-app");
        SysRole role = new SysRole();
        role.setId(20);
        when(configReader.register()).thenReturn(registration);
        when(clientManager.requireSettings()).thenReturn(wechat);
        when(roleService.getByCode("user")).thenReturn(role);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(42);
            return true;
        }).when(userService).save(any(SysUser.class));
        when(identityService.save(any(UserIdentity.class))).thenReturn(true);
        WechatLoginService service = new WechatLoginService(
                mock(RedisCache.class), configReader, clientManager, identityService,
                userService, roleService, mock(AuthBizService.class));

        SysUser result = service.resolveUser("openid-1");

        assertThat(result.getId()).isEqualTo(42);
        assertThat(result.getEmail()).isNull();
        assertThat(result.getPassword()).isNull();
        assertThat(result.getNickname()).matches("微信用户-[A-Z2-9]{6}");
        assertThat(result.getLoginType()).isEqualTo(LoginTypeEnum.WECHAT_MP.getCode());
        assertThat(result.getStatus()).isEqualTo(SysUserStatusEnum.PENDING.getCode());
        verify(roleService).addUserRoles(42, List.of(20));
        ArgumentCaptor<UserIdentity> identityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identityService).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue())
                .extracting(UserIdentity::getUserId, UserIdentity::getProvider,
                        UserIdentity::getProviderAppId, UserIdentity::getProviderUserId)
                .containsExactly(42, WechatLoginService.PROVIDER, "wx-app", "openid-1");
    }
}
