package com.nexora.monitor.infrastructure.serverssh;

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
public class TerminalTicketStore {

    public static final String HANDSHAKE_TICKET_ATTRIBUTE =
            TerminalTicketStore.class.getName() + ".ticket";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, TerminalTicket> tickets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform().daemon().name("ssh-ticket-cleanup").factory());

    @PostConstruct
    void startCleanup() {
        cleanupExecutor.scheduleAtFixedRate(this::removeExpired, 15, 15, TimeUnit.SECONDS);
    }

    public TerminalTicket issue(Integer ownerId, Long serverId, String password,
                                int columns, int rows) {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        TerminalTicket ticket = new TerminalTicket(
                value, ownerId, serverId, password, columns, rows,
                Instant.now().plus(TICKET_TTL));
        tickets.put(value, ticket);
        return ticket;
    }

    public TerminalTicket consume(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        TerminalTicket ticket = tickets.remove(value);
        if (ticket == null || !ticket.expiresAt().isAfter(Instant.now())) {
            return null;
        }
        return ticket;
    }

    public void removeByOwnerId(Integer ownerId) {
        tickets.entrySet().removeIf(entry -> entry.getValue().ownerId().equals(ownerId));
    }

    public void removeByServer(Integer ownerId, Long serverId) {
        tickets.entrySet().removeIf(entry ->
                entry.getValue().ownerId().equals(ownerId)
                        && entry.getValue().serverId().equals(serverId));
    }

    private void removeExpired() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    @PreDestroy
    void shutdown() {
        cleanupExecutor.shutdownNow();
        tickets.clear();
    }

    public record TerminalTicket(
            String value,
            Integer ownerId,
            Long serverId,
            String password,
            int columns,
            int rows,
            Instant expiresAt) {
    }
}
