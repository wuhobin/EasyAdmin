package com.nexora.security.session;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OnlineSessionRecordTest {

    @Test
    void createsCanonicalUuidV4SessionIds() {
        String sessionId = OnlineSessionRecord.createSessionId();

        assertThat(UUID.fromString(sessionId).version()).isEqualTo(4);
        assertThat(UUID.fromString(sessionId).toString()).isEqualTo(sessionId);
    }

    @Test
    void rejectsNonV4SessionIds() {
        assertThatIllegalArgumentException().isThrownBy(() -> new OnlineSessionRecord(
                "00000000-0000-1000-8000-000000000000",
                7,
                "user@example.com",
                null,
                "127.0.0.1",
                null,
                null,
                LocalDateTime.now()));
    }

    @Test
    void neverDeclaresCredentialFields() {
        assertThat(Arrays.stream(OnlineSessionRecord.class.getRecordComponents())
                .map(RecordComponent::getName))
                .noneMatch(name -> name.toLowerCase().contains("token"));
    }
}
