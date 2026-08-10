package com.nexora.monitor.infrastructure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds server-resolved operation audit details for the current request thread.
 */
public final class OperationLogContext {

    private static final String TARGET_USER_ID = "targetUserId";
    private static final String TARGET_EMAIL = "targetEmail";
    private static final String TARGET_SESSION_ID = "targetSessionId";
    private static final String TARGET_IP = "targetIp";
    private static final String OUTCOME = "outcome";

    private static final ThreadLocal<AuditData> AUDIT_DATA = new ThreadLocal<>();

    private OperationLogContext() {
    }

    public static void setTarget(
            Integer userId,
            String email,
            String sessionId,
            String ip) {
        AuditData current = AUDIT_DATA.get();
        AUDIT_DATA.set(new AuditData(
                userId,
                email,
                sessionId,
                ip,
                current == null ? null : current.outcome()));
    }

    public static void setOutcome(String outcome) {
        AuditData current = AUDIT_DATA.get();
        if (current == null) {
            AUDIT_DATA.set(new AuditData(null, null, null, null, outcome));
            return;
        }
        AUDIT_DATA.set(new AuditData(
                current.targetUserId(),
                current.targetEmail(),
                current.targetSessionId(),
                current.targetIp(),
                outcome));
    }

    public static Map<String, Object> parameters() {
        AuditData data = AUDIT_DATA.get();
        if (data == null) {
            return Map.of();
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        putIfPresent(parameters, TARGET_USER_ID, data.targetUserId());
        putIfPresent(parameters, TARGET_EMAIL, data.targetEmail());
        putIfPresent(parameters, TARGET_SESSION_ID, data.targetSessionId());
        putIfPresent(parameters, TARGET_IP, data.targetIp());
        putIfPresent(parameters, OUTCOME, data.outcome());
        return Collections.unmodifiableMap(parameters);
    }

    public static void clear() {
        AUDIT_DATA.remove();
    }

    private static void putIfPresent(
            Map<String, Object> parameters,
            String key,
            Object value) {
        if (value != null) {
            parameters.put(key, value);
        }
    }

    private record AuditData(
            Integer targetUserId,
            String targetEmail,
            String targetSessionId,
            String targetIp,
            String outcome) {
    }
}
