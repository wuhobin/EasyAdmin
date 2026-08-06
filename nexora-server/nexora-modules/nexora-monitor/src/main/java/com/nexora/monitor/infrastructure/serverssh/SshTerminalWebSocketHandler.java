package com.nexora.monitor.infrastructure.serverssh;

import com.aurora.starter.webmvc.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.monitor.constants.ServerConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
public class SshTerminalWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_MESSAGE_LENGTH = 70_000;

    private final SshTerminalSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public SshTerminalWebSocketHandler(SshTerminalSessionManager sessionManager,
                                       ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object attribute = session.getAttributes()
                .get(TerminalTicketStore.HANDSHAKE_TICKET_ATTRIBUTE);
        if (!(attribute instanceof TerminalTicketStore.TerminalTicket ticket)) {
            sessionManager.sendProtocolError(session, ServerConstants.SSH_TICKET_INVALID_MESSAGE);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        try {
            sessionManager.open(session, ticket);
        } catch (BizException exception) {
            log.error("SSH terminal connection failed, sessionId={}", session.getId(), exception);
            sessionManager.sendProtocolError(session, exception.getMessage());
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (message.getPayloadLength() > MAX_MESSAGE_LENGTH) {
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        JsonNode payload;
        try {
            payload = objectMapper.readTree(message.getPayload());
        } catch (Exception exception) {
            sessionManager.sendProtocolError(session, "终端消息格式不正确");
            return;
        }
        String type = payload.path("type").asText();
        if ("data".equals(type)) {
            sessionManager.writeInput(session.getId(), payload.path("data").asText());
        } else if ("resize".equals(type)) {
            sessionManager.resize(
                    session.getId(),
                    payload.path("columns").asInt(80),
                    payload.path("rows").asInt(24));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.close(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionManager.close(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
