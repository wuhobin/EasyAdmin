package com.nexora.monitor.infrastructure.serverssh;

import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.monitor.constants.ServerConstants;
import com.nexora.monitor.entity.ManagedServer;
import com.nexora.monitor.service.ManagedServerService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.sshd.client.channel.ChannelShell;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class SshTerminalSessionManager {

    private static final int MAX_TERMINALS_PER_USER = 3;
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration CHANNEL_OPEN_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_INPUT_LENGTH = 65_536;

    private final ManagedServerService serverService;
    private final SshConnectionService connectionService;
    private final ObjectMapper objectMapper;
    private final Map<String, TerminalSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> activeSessionsByOwner = new HashMap<>();
    private final ScheduledExecutorService idleExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform().daemon().name("ssh-terminal-idle").factory());

    public SshTerminalSessionManager(ManagedServerService serverService,
                                     SshConnectionService connectionService,
                                     ObjectMapper objectMapper) {
        this.serverService = serverService;
        this.connectionService = connectionService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void startIdleChecks() {
        idleExecutor.scheduleAtFixedRate(this::closeIdleSessions, 1, 1, TimeUnit.MINUTES);
    }

    public int activeCount(Integer ownerId) {
        synchronized (activeSessionsByOwner) {
            return activeSessionsByOwner.getOrDefault(ownerId, Set.of()).size();
        }
    }

    public void open(WebSocketSession webSocket, TerminalTicketStore.TerminalTicket ticket) {
        if (!reserve(ticket.ownerId(), webSocket.getId())) {
            throw new BizException(ServerConstants.SSH_TERMINAL_LIMIT_MESSAGE);
        }

        ManagedServer server = serverService.getByIdAndOwnerId(ticket.serverId(), ticket.ownerId());
        if (server == null) {
            release(ticket.ownerId(), webSocket.getId());
            throw new BizException(ServerConstants.SERVER_UNAVAILABLE_MESSAGE);
        }
        if (!Integer.valueOf(1).equals(server.getEnabled())) {
            release(ticket.ownerId(), webSocket.getId());
            throw new BizException(ServerConstants.SERVER_DISABLED_MESSAGE);
        }
        if (server.getTrustedFingerprint() == null || server.getTrustedFingerprint().isBlank()) {
            release(ticket.ownerId(), webSocket.getId());
            throw new BizException(ServerConstants.SSH_HOST_KEY_REQUIRED_MESSAGE);
        }

        AuthenticatedSshConnection connection = null;
        ChannelShell channel = null;
        try {
            connection = connectionService.authenticate(server, ticket.password());
            channel = connection.session().createShellChannel();
            channel.setPtyType("xterm-256color");
            channel.setPtyColumns(ticket.columns());
            channel.setPtyLines(ticket.rows());
            channel.open().verify(CHANNEL_OPEN_TIMEOUT);

            TerminalSession terminalSession = new TerminalSession(
                    webSocket, ticket.ownerId(), ticket.serverId(), connection, channel,
                    channel.getInvertedOut(), channel.getInvertedIn());
            sessions.put(webSocket.getId(), terminalSession);
            serverService.updateConnectionState(ticket.serverId(), ticket.ownerId(), "");
            send(webSocket, Map.of("type", "connected"));

            Thread reader = Thread.ofVirtual()
                    .name("ssh-terminal-reader-" + webSocket.getId())
                    .unstarted(() -> readOutput(webSocket.getId(), terminalSession));
            terminalSession.reader = reader;
            reader.start();
        } catch (SshHostKeyMismatchException exception) {
            closeQuietly(channel, connection);
            release(ticket.ownerId(), webSocket.getId());
            serverService.updateConnectionState(
                    ticket.serverId(), ticket.ownerId(), ServerConstants.SSH_HOST_KEY_CHANGED_MESSAGE);
            throw new BizException(ServerConstants.SSH_HOST_KEY_CHANGED_MESSAGE);
        } catch (BizException exception) {
            closeQuietly(channel, connection);
            release(ticket.ownerId(), webSocket.getId());
            serverService.updateConnectionState(
                    ticket.serverId(), ticket.ownerId(), exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            closeQuietly(channel, connection);
            release(ticket.ownerId(), webSocket.getId());
            serverService.updateConnectionState(
                    ticket.serverId(), ticket.ownerId(), ServerConstants.SSH_CONNECTION_FAILED_MESSAGE);
            throw new BizException(ServerConstants.SSH_CONNECTION_FAILED_MESSAGE);
        }
    }

    public void writeInput(String webSocketSessionId, String data) {
        TerminalSession session = sessions.get(webSocketSessionId);
        if (session == null || data == null || data.length() > MAX_INPUT_LENGTH) {
            return;
        }
        try {
            session.output.write(data.getBytes(StandardCharsets.UTF_8));
            session.output.flush();
            session.lastInputAt = Instant.now();
        } catch (IOException exception) {
            close(webSocketSessionId);
        }
    }

    public void resize(String webSocketSessionId, int columns, int rows) {
        TerminalSession session = sessions.get(webSocketSessionId);
        if (session == null) {
            return;
        }
        int safeColumns = Math.max(20, Math.min(columns, 500));
        int safeRows = Math.max(5, Math.min(rows, 200));
        try {
            session.channel.sendWindowChange(safeColumns, safeRows);
        } catch (IOException exception) {
            close(webSocketSessionId);
        }
    }

    public void close(String webSocketSessionId) {
        TerminalSession session = sessions.remove(webSocketSessionId);
        if (session != null) {
            release(session.ownerId, webSocketSessionId);
            closeResources(session);
        }
    }

    public void closeByServer(Integer ownerId, Long serverId) {
        new ArrayList<>(sessions.entrySet()).forEach(entry -> {
            TerminalSession session = entry.getValue();
            if (session.ownerId.equals(ownerId) && session.serverId.equals(serverId)) {
                closeWithMessage(entry.getKey(), "服务器配置已变更，终端已断开");
            }
        });
    }

    public void closeByOwnerId(Integer ownerId) {
        closeByOwnerId(ownerId, "用户已删除，终端已断开");
    }

    public void closeByDisabledOwnerId(Integer ownerId) {
        closeByOwnerId(ownerId, "用户已禁用，终端已断开");
    }

    private void closeByOwnerId(Integer ownerId, String message) {
        new ArrayList<>(sessions.entrySet()).forEach(entry -> {
            if (entry.getValue().ownerId.equals(ownerId)) {
                closeWithMessage(entry.getKey(), message);
            }
        });
    }

    public void sendProtocolError(WebSocketSession webSocket, String message) {
        send(webSocket, Map.of("type", "error", "message", message));
    }

    private void readOutput(String sessionId, TerminalSession terminalSession) {
        byte[] buffer = new byte[4096];
        try {
            int length;
            while ((length = terminalSession.input.read(buffer)) >= 0) {
                if (length == 0 || !terminalSession.webSocket.isOpen()) {
                    continue;
                }
                String encoded = Base64.getEncoder().encodeToString(
                        java.util.Arrays.copyOf(buffer, length));
                send(terminalSession.webSocket,
                        Map.of("type", "data", "encoding", "base64", "data", encoded));
            }
        } catch (IOException ignored) {
            // Closing the SSH channel interrupts the reader as part of normal cleanup.
        } finally {
            closeWithMessage(sessionId, "SSH 连接已关闭");
        }
    }

    private void closeIdleSessions() {
        Instant threshold = Instant.now().minus(IDLE_TIMEOUT);
        new ArrayList<>(sessions.entrySet()).forEach(entry -> {
            if (entry.getValue().lastInputAt.isBefore(threshold)) {
                closeWithMessage(entry.getKey(), "30 分钟无输入，终端已自动断开");
            }
        });
    }

    private void closeWithMessage(String sessionId, String message) {
        TerminalSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        release(session.ownerId, sessionId);
        try {
            if (session.webSocket.isOpen()) {
                send(session.webSocket, Map.of("type", "disconnected", "message", message));
                session.webSocket.close(CloseStatus.NORMAL);
            }
        } catch (IOException ignored) {
            // Resource cleanup below is authoritative.
        } finally {
            closeResources(session);
        }
    }

    private boolean reserve(Integer ownerId, String sessionId) {
        synchronized (activeSessionsByOwner) {
            Set<String> active = activeSessionsByOwner.computeIfAbsent(ownerId, ignored -> new HashSet<>());
            if (active.size() >= MAX_TERMINALS_PER_USER) {
                return false;
            }
            active.add(sessionId);
            return true;
        }
    }

    private void release(Integer ownerId, String sessionId) {
        synchronized (activeSessionsByOwner) {
            Set<String> active = activeSessionsByOwner.get(ownerId);
            if (active == null) {
                return;
            }
            active.remove(sessionId);
            if (active.isEmpty()) {
                activeSessionsByOwner.remove(ownerId);
            }
        }
    }

    private void send(WebSocketSession webSocket, Map<String, ?> payload) {
        if (!webSocket.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (webSocket) {
                if (webSocket.isOpen()) {
                    webSocket.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException ignored) {
            close(webSocket.getId());
        }
    }

    private static void closeResources(TerminalSession session) {
        Thread reader = session.reader;
        if (reader != null && reader != Thread.currentThread()) {
            reader.interrupt();
        }
        closeQuietly(session.channel, session.connection);
    }

    private static void closeQuietly(ChannelShell channel, AuthenticatedSshConnection connection) {
        if (channel != null) {
            try {
                channel.close(false);
            } catch (Exception ignored) {
                // Continue with the SSH client cleanup.
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
                // The session is already unusable.
            }
        }
    }

    @PreDestroy
    void shutdown() {
        idleExecutor.shutdownNow();
        new ArrayList<>(sessions.keySet()).forEach(this::close);
    }

    private static final class TerminalSession {
        private final WebSocketSession webSocket;
        private final Integer ownerId;
        private final Long serverId;
        private final AuthenticatedSshConnection connection;
        private final ChannelShell channel;
        private final InputStream input;
        private final OutputStream output;
        private volatile Instant lastInputAt = Instant.now();
        private volatile Thread reader;

        private TerminalSession(WebSocketSession webSocket, Integer ownerId, Long serverId,
                                AuthenticatedSshConnection connection, ChannelShell channel,
                                InputStream input, OutputStream output) {
            this.webSocket = webSocket;
            this.ownerId = ownerId;
            this.serverId = serverId;
            this.connection = connection;
            this.channel = channel;
            this.input = input;
            this.output = output;
        }
    }
}
