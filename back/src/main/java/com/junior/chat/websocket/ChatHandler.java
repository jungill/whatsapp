package com.junior.chat.websocket;

import tools.jackson.databind.ObjectMapper; // tools.jackson.databind.ObjectMapper si Spring Boot 4
import com.junior.chat.model.ChatMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception { // enregistre la connexion WebSocket pour l'utilisateur en fait une liste de sessions WebSocket ouvertes
        String userId = userId(session);
        if (userId == null || userId.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("userId manquant"));
            return;
        }
        sessions.put(userId, new ConcurrentWebSocketSessionDecorator(session, 5000, 65536));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatMessage in = mapper.readValue(message.getPayload(), ChatMessage.class);
        ChatMessage out = new ChatMessage(in.id(), userId(session), in.to(), in.content());

        WebSocketSession target = sessions.get(in.to());
        if (target != null && target.isOpen()) {
            target.sendMessage(new TextMessage(mapper.writeValueAsString(out)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = userId(session);
        if (userId != null) {
            sessions.remove(userId);
        }
    }

    private String userId(WebSocketSession session) {
        return UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("userId");
    }
}