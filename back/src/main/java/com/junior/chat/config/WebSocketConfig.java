package com.junior.chat.config;

import com.junior.chat.websocket.ChatHandler;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.junior.chat.security.JwtHandshakeInterceptor;

@Slf4j   // Lombok : crée automatiquement un logger nommé "log"
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatHandler chatHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(ChatHandler chatHandler, JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.chatHandler = chatHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        log.info(">>> WebSocketConfig créé : {}", this);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}