package com.nexora.security.session;

import com.nexora.handler.onlineuser.OnlineSessionRegistry;
import com.nexora.handler.onlineuser.OnlineSessionTokenResolver;
import com.nexora.handler.onlineuser.OnlineSessionTouchInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OnlineSessionTouchInterceptorTest {

    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

    private final OnlineSessionTokenResolver tokenResolver =
            mock(OnlineSessionTokenResolver.class);
    private final OnlineSessionRegistry onlineSessionRegistry =
            mock(OnlineSessionRegistry.class);
    private final OnlineSessionTouchInterceptor interceptor =
            new OnlineSessionTouchInterceptor(tokenResolver, onlineSessionRegistry);

    @Test
    void touchesTheCurrentPublicSessionForAnAuthenticatedRequest() {
        when(tokenResolver.currentSessionId()).thenReturn(Optional.of(SESSION_ID));
        when(tokenResolver.currentTokenTimeout()).thenReturn(300L);
        long before = System.currentTimeMillis();

        assertThat(preHandle()).isTrue();

        long after = System.currentTimeMillis();
        ArgumentCaptor<Long> accessTime = ArgumentCaptor.forClass(Long.class);
        verify(onlineSessionRegistry).touch(
                org.mockito.ArgumentMatchers.eq(SESSION_ID),
                accessTime.capture(),
                org.mockito.ArgumentMatchers.eq(300L));
        assertThat(accessTime.getValue()).isBetween(before, after);
    }

    @Test
    void skipsLegacyOrUnauthenticatedRequestsWithoutAPublicSessionId() {
        when(tokenResolver.currentSessionId()).thenReturn(Optional.empty());

        assertThat(preHandle()).isTrue();

        verify(tokenResolver, never()).currentTokenTimeout();
        verifyNoInteractions(onlineSessionRegistry);
    }

    @Test
    void allowsTheBusinessRequestWhenRedisTouchFails() {
        when(tokenResolver.currentSessionId()).thenReturn(Optional.of(SESSION_ID));
        when(tokenResolver.currentTokenTimeout()).thenReturn(300L);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(onlineSessionRegistry)
                .touch(org.mockito.ArgumentMatchers.eq(SESSION_ID), anyLong(),
                        org.mockito.ArgumentMatchers.eq(300L));

        assertThat(preHandle()).isTrue();
    }

    private boolean preHandle() {
        return interceptor.preHandle(
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class),
                new Object());
    }
}
