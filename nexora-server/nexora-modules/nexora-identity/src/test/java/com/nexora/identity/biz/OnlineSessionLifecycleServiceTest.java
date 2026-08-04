package com.nexora.identity.biz;

import com.aurora.starter.security.context.SecurityUtils;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.infrastructure.LoginClientInfoResolver;
import com.nexora.security.session.OnlineSessionRecord;
import com.nexora.security.session.OnlineSessionRegistry;
import com.nexora.security.session.OnlineSessionTokenResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnlineSessionLifecycleServiceTest {

    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

    private final OnlineSessionRegistry registry = mock(OnlineSessionRegistry.class);
    private final OnlineSessionTokenResolver tokenResolver = mock(OnlineSessionTokenResolver.class);
    private final LoginClientInfoResolver clientInfoResolver = mock(LoginClientInfoResolver.class);
    private final OnlineSessionLifecycleService service =
            new OnlineSessionLifecycleService(registry, tokenResolver, clientInfoResolver);

    @Test
    void createsIndependentUuidV4SessionIds() {
        String first = service.createSessionId();
        String second = service.createSessionId();

        assertThat(first).isNotEqualTo(second);
        assertThat(UUID.fromString(first).version()).isEqualTo(4);
        assertThat(UUID.fromString(second).version()).isEqualTo(4);
    }

    @Test
    void registersCredentialFreeUserAndClientSnapshotWithRemainingTokenTtl() {
        SysUser user = SysUser.builder()
                .id(7)
                .email("user@example.com")
                .nickname("User")
                .build();
        when(clientInfoResolver.resolve()).thenReturn(
                new LoginClientInfoResolver.ClientInfo(
                        "203.0.113.8", "Chrome 140.0.0.0", "Windows 10.0"));
        when(tokenResolver.currentTokenTimeout()).thenReturn(120L);
        LocalDateTime before = LocalDateTime.now();

        service.register(user, SESSION_ID);

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<OnlineSessionRecord> captor =
                ArgumentCaptor.forClass(OnlineSessionRecord.class);
        verify(registry).register(captor.capture(), org.mockito.ArgumentMatchers.eq(120L));
        OnlineSessionRecord record = captor.getValue();
        assertThat(record.sessionId()).isEqualTo(SESSION_ID);
        assertThat(record.userId()).isEqualTo(7);
        assertThat(record.email()).isEqualTo("user@example.com");
        assertThat(record.nickname()).isEqualTo("User");
        assertThat(record.ip()).isEqualTo("203.0.113.8");
        assertThat(record.browser()).isEqualTo("Chrome 140.0.0.0");
        assertThat(record.os()).isEqualTo("Windows 10.0");
        assertThat(record.loginTime()).isBetween(before, after);
    }

    @Test
    void logsOutTheNewTokenWhenRegistryPersistenceFails() {
        SysUser user = SysUser.builder()
                .id(7)
                .email("user@example.com")
                .build();
        when(clientInfoResolver.resolve()).thenReturn(
                new LoginClientInfoResolver.ClientInfo("127.0.0.1", null, null));
        when(tokenResolver.currentTokenTimeout()).thenReturn(120L);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(registry).register(any(OnlineSessionRecord.class), org.mockito.ArgumentMatchers.eq(120L));
        when(tokenResolver.logoutSession(7, SESSION_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.register(user, SESSION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis unavailable");

        verify(tokenResolver).logoutSession(7, SESSION_ID);
    }

    @Test
    void capturesCurrentSessionBeforeLogoutAndRemovesOnlyThatRecord() {
        when(tokenResolver.currentSessionId()).thenReturn(Optional.of(SESSION_ID));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            service.logoutCurrentSession();

            securityUtils.verify(SecurityUtils::logout);
            verify(registry).remove(SESSION_ID);
            verify(registry, never()).removeByUserId(any());
        }
    }

    @Test
    void invalidatesAllTokensBeforeRemovingTheUsersSessionRecords() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            service.invalidateUserSessions(7);

            securityUtils.verify(() -> SecurityUtils.kickout(7));
            verify(registry).removeByUserId(7);
        }
    }
}
