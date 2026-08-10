package com.nexora.monitor.config;

import com.nexora.monitor.infrastructure.serverssh.SshTerminalHandshakeInterceptor;
import com.nexora.monitor.infrastructure.serverssh.SshTerminalWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class SshTerminalWebSocketConfig implements WebSocketConfigurer {

    private final SshTerminalWebSocketHandler handler;
    private final SshTerminalHandshakeInterceptor handshakeInterceptor;

    public SshTerminalWebSocketConfig(SshTerminalWebSocketHandler handler,
                                      SshTerminalHandshakeInterceptor handshakeInterceptor) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/ssh")
                .addInterceptors(handshakeInterceptor);
    }
}
