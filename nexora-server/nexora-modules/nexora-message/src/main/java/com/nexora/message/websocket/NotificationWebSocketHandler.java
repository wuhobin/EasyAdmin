package com.nexora.message.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final NotificationWebSocketSessionManager sessionManager;

    public NotificationWebSocketHandler(ObjectMapper objectMapper,
                                        NotificationWebSocketSessionManager sessionManager) {
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object value = session.getAttributes().get(NotificationTicketStore.HANDSHAKE_ATTRIBUTE);
        if (!(value instanceof NotificationTicketStore.Ticket ticket)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessionManager.open(session, ticket.userId());
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("event", "connected"))));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            if ("ping".equals(node.path("type").asText())) {
                synchronized (session) {
                    session.sendMessage(new TextMessage("{\"event\":\"pong\"}"));
                }
            }
        } catch (Exception ignored) {
            // Unknown heartbeat payloads are ignored to keep the channel non-blocking.
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.close(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionManager.close(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
