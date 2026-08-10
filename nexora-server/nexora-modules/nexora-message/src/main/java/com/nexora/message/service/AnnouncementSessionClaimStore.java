package com.nexora.message.service;

import cn.dev33.satoken.dao.SaTokenDao;
import com.nexora.handler.onlineuser.OnlineSessionTokenResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AnnouncementSessionClaimStore {
    private final OnlineSessionTokenResolver tokenResolver;
    private final Clock clock;
    private final Map<String, Instant> claims = new ConcurrentHashMap<>();

    @Autowired
    public AnnouncementSessionClaimStore(OnlineSessionTokenResolver tokenResolver) {
        this(tokenResolver, Clock.systemUTC());
    }

    AnnouncementSessionClaimStore(OnlineSessionTokenResolver tokenResolver, Clock clock) {
        this.tokenResolver = tokenResolver;
        this.clock = clock;
    }

    public boolean claimCurrentSession() {
        String sessionId = tokenResolver.currentSessionId().orElse(null);
        if (sessionId == null) {
            return false;
        }
        long timeout = tokenResolver.currentTokenTimeout();
        if (timeout != SaTokenDao.NEVER_EXPIRE && timeout <= 0) {
            return false;
        }
        Instant now = clock.instant();
        claims.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        Instant expiresAt = timeout == SaTokenDao.NEVER_EXPIRE
                ? Instant.MAX : now.plusSeconds(timeout);
        AtomicBoolean claimed = new AtomicBoolean(false);
        claims.compute(sessionId, (ignored, existing) -> {
            if (existing != null && existing.isAfter(now)) {
                return existing;
            }
            claimed.set(true);
            return expiresAt;
        });
        return claimed.get();
    }
}
