package com.nexora.security.session;

import com.aurora.starter.redis.core.JsonRedisTemplate;
import com.nexora.handler.onlineuser.OnlineSessionRecord;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

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

    @Test
    void roundTripsThroughStarterRedisSerializer() {
        OnlineSessionRecord record = new OnlineSessionRecord(
                OnlineSessionRecord.createSessionId(),
                7,
                "user@example.com",
                "User",
                "127.0.0.1",
                "Chrome",
                "Windows",
                LocalDateTime.of(2026, 8, 5, 13, 30, 45, 123_000_000));
        JsonRedisTemplate redisTemplate =
                new JsonRedisTemplate(mock(RedisConnectionFactory.class));
        GenericJackson2JsonRedisSerializer serializer =
                (GenericJackson2JsonRedisSerializer) redisTemplate.getValueSerializer();

        Object restored = serializer.deserialize(serializer.serialize(record));

        assertThat(restored).isEqualTo(record);
    }
}
