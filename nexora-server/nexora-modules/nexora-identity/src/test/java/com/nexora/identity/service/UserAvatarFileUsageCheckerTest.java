package com.nexora.identity.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAvatarFileUsageCheckerTest {

    @Test
    void delegatesFileUsageCheckToUserService() {
        SysUserService userService = mock(SysUserService.class);
        when(userService.existsByAvatar("https://oss.example.com/avatar.png")).thenReturn(true);
        UserAvatarFileUsageChecker checker = new UserAvatarFileUsageChecker(userService);

        assertThat(checker.isInUse("https://oss.example.com/avatar.png")).isTrue();
        verify(userService).existsByAvatar("https://oss.example.com/avatar.png");
    }
}
