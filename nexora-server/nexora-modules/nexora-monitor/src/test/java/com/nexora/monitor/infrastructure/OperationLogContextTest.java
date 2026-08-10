package com.nexora.monitor.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLogContextTest {

    @AfterEach
    void clearContext() {
        OperationLogContext.clear();
    }

    @Test
    void exposesOnlyTheServerResolvedAuditTargetAndOutcome() {
        OperationLogContext.setTarget(
                42,
                "target@example.com",
                "550e8400-e29b-41d4-a716-446655440000",
                "203.0.113.8");
        OperationLogContext.setOutcome("LOGGED_OUT");

        assertThat(OperationLogContext.parameters())
                .containsEntry("targetUserId", 42)
                .containsEntry("targetEmail", "target@example.com")
                .containsEntry(
                        "targetSessionId",
                        "550e8400-e29b-41d4-a716-446655440000")
                .containsEntry("targetIp", "203.0.113.8")
                .containsEntry("outcome", "LOGGED_OUT");
    }

    @Test
    void supportsAnIdempotentOutcomeWithoutInventingTargetData() {
        OperationLogContext.setOutcome("ALREADY_OFFLINE");

        assertThat(OperationLogContext.parameters())
                .containsOnlyKeys("outcome")
                .containsEntry("outcome", "ALREADY_OFFLINE");
    }

    @Test
    void clearsThreadStateAndHasNoTokenFieldContract() {
        OperationLogContext.setTarget(
                42,
                "target@example.com",
                "550e8400-e29b-41d4-a716-446655440000",
                "203.0.113.8");

        OperationLogContext.clear();

        assertThat(OperationLogContext.parameters()).isEmpty();
        assertThat(Arrays.stream(OperationLogContext.class.getDeclaredClasses())
                .flatMap(type -> Arrays.stream(type.getDeclaredFields()))
                .map(java.lang.reflect.Field::getName))
                .noneMatch(name -> name.toLowerCase().contains("token"));
    }
}
