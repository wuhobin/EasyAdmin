package com.nexora.identity.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationTicketStore {
    public static final String HANDSHAKE_ATTRIBUTE = NotificationTicketStore.class.getName() + ".ticket";
    private static final Duration TTL = Duration.ofSeconds(30);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("notification-ticket-cleanup").factory());

    @PostConstruct
    void start() {
        cleanup.scheduleAtFixedRate(this::removeExpired, 15, 15, TimeUnit.SECONDS);
    }

    public Ticket issue(Integer userId) {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        Ticket ticket = new Ticket(userId, Instant.now().plus(TTL));
        tickets.put(token, ticket);
        return new Ticket(token, ticket.userId(), ticket.expiresAt());
    }

    public Ticket consume(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Ticket ticket = tickets.remove(token);
        return ticket == null || !ticket.expiresAt().isAfter(Instant.now()) ? null : ticket;
    }

    private void removeExpired() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    @PreDestroy
    void stop() {
        cleanup.shutdownNow();
        tickets.clear();
    }

    public record Ticket(String value, Integer userId, Instant expiresAt) {
        private Ticket(Integer userId, Instant expiresAt) {
            this(null, userId, expiresAt);
        }
    }
}
