package com.nexora.identity.biz;

import com.aurora.starter.security.context.SecurityUtils;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.infrastructure.LoginClientInfoResolver;
import com.nexora.security.session.OnlineSessionRecord;
import com.nexora.security.session.OnlineSessionRegistry;
import com.nexora.security.session.OnlineSessionTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Maintains online-session metadata alongside identity-owned token lifecycle operations.
 */
@Service
@RequiredArgsConstructor
public class OnlineSessionLifecycleService {

    private final OnlineSessionRegistry onlineSessionRegistry;
    private final OnlineSessionTokenResolver tokenResolver;
    private final LoginClientInfoResolver clientInfoResolver;

    public String createSessionId() {
        return OnlineSessionRecord.createSessionId();
    }

    public void register(SysUser user, String sessionId) {
        try {
            LoginClientInfoResolver.ClientInfo clientInfo = clientInfoResolver.resolve();
            OnlineSessionRecord record = new OnlineSessionRecord(
                    sessionId,
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    clientInfo.ip(),
                    clientInfo.browser(),
                    clientInfo.os(),
                    LocalDateTime.now());
            onlineSessionRegistry.register(record, tokenResolver.currentTokenTimeout());
        } catch (RuntimeException exception) {
            rollbackNewSession(user.getId(), sessionId, exception);
            throw exception;
        }
    }

    /**
     * Invalidates a newly created token when login initialization cannot reach registration.
     */
    public void rollbackUnregisteredSession(
            Integer userId,
            String sessionId,
            RuntimeException originalException) {
        rollbackNewSession(userId, sessionId, originalException);
    }

    public void logoutCurrentSession() {
        Optional<String> sessionId = tokenResolver.currentSessionId();
        SecurityUtils.logout();
        sessionId.ifPresent(onlineSessionRegistry::remove);
    }

    public void invalidateUserSessions(Integer userId) {
        SecurityUtils.kickout(userId);
        onlineSessionRegistry.removeByUserId(userId);
    }

    private void rollbackNewSession(Integer userId, String sessionId, RuntimeException originalException) {
        try {
            if (!tokenResolver.logoutSession(userId, sessionId)) {
                SecurityUtils.logout();
            }
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }
}
