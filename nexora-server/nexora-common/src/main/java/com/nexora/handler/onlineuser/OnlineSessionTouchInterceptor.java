package com.nexora.handler.onlineuser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Best-effort last-access tracking for authenticated MVC requests.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineSessionTouchInterceptor implements HandlerInterceptor {

    private final OnlineSessionTokenResolver tokenResolver;
    private final OnlineSessionRegistry onlineSessionRegistry;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        try {
            tokenResolver.currentSessionId().ifPresent(this::touch);
        } catch (RuntimeException exception) {
            log.warn("Failed to update online-session last-access time; "
                    + "request will continue (failure type: {})",
                    exception.getClass().getName());
        }
        return true;
    }

    private void touch(String sessionId) {
        onlineSessionRegistry.touch(
                sessionId,
                System.currentTimeMillis(),
                tokenResolver.currentTokenTimeout());
    }
}
