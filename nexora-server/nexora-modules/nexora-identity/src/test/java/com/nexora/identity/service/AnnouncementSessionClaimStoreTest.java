package com.nexora.identity.service;

import com.nexora.handler.onlineuser.OnlineSessionTokenResolver;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnnouncementSessionClaimStoreTest {
    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    void canBeCreatedByTheSpringContainer() {
        OnlineSessionTokenResolver resolver = mock(OnlineSessionTokenResolver.class);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(OnlineSessionTokenResolver.class, () -> resolver);
            context.register(AnnouncementSessionClaimStore.class);
            context.refresh();

            assertThat(context.getBean(AnnouncementSessionClaimStore.class)).isNotNull();
        }
    }

    @Test
    void claimsOnlyOnceUntilTheActualLoginSessionExpires() {
        OnlineSessionTokenResolver resolver = mock(OnlineSessionTokenResolver.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-08-08T00:00:00Z"));
        when(resolver.currentSessionId()).thenReturn(Optional.of(SESSION_ID));
        when(resolver.currentTokenTimeout()).thenReturn(100L);
        AnnouncementSessionClaimStore store = new AnnouncementSessionClaimStore(resolver, clock);

        assertThat(store.claimCurrentSession()).isTrue();
        assertThat(store.claimCurrentSession()).isFalse();

        clock.advanceSeconds(101);
        assertThat(store.claimCurrentSession()).isTrue();
    }

    @Test
    void keepsTheClaimForSessionsThatNeverExpire() {
        OnlineSessionTokenResolver resolver = mock(OnlineSessionTokenResolver.class);
        when(resolver.currentSessionId()).thenReturn(Optional.of(SESSION_ID));
        when(resolver.currentTokenTimeout()).thenReturn(-1L);
        AnnouncementSessionClaimStore store = new AnnouncementSessionClaimStore(
                resolver, Clock.fixed(Instant.now(), ZoneId.of("UTC")));

        assertThat(store.claimCurrentSession()).isTrue();
        assertThat(store.claimCurrentSession()).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
