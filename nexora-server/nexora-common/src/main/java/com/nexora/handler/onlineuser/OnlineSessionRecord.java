package com.nexora.handler.onlineuser;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A credential-free snapshot of one authenticated token session.
 */
public record OnlineSessionRecord(
        String sessionId,
        Integer userId,
        String email,
        String nickname,
        String ip,
        String browser,
        String os,
        LocalDateTime loginTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public OnlineSessionRecord {
        sessionId = requireUuidV4(sessionId);
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        email = normalizeOptional(email);
        ip = requireText(ip, "ip");
        nickname = normalizeOptional(nickname);
        browser = normalizeOptional(browser);
        os = normalizeOptional(os);
        loginTime = Objects.requireNonNull(loginTime, "loginTime must not be null");
    }

    public static String createSessionId() {
        return UUID.randomUUID().toString();
    }

    private static String requireUuidV4(String value) {
        String normalized = requireText(value, "sessionId");
        UUID uuid;
        try {
            uuid = UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("sessionId must be a UUID", exception);
        }
        if (uuid.version() != 4 || !uuid.toString().equalsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("sessionId must be a canonical UUID v4");
        }
        return uuid.toString();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
