package com.interpreter.aibackend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AiInterpreterHandler aiInterpreterHandler;

    public WebSocketConfig(AiInterpreterHandler aiInterpreterHandler) {
        this.aiInterpreterHandler = aiInterpreterHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 允许前端连接到 ws://localhost:8080/ws/interpreter
        registry.addHandler(aiInterpreterHandler, "/ws/interpreter")
                .setAllowedOrigins("*"); // 开发阶段允许跨域
    }
}