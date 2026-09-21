package com.junior.chat.websocket;

import tools.jackson.databind.ObjectMapper; // tools.jackson.databind.ObjectMapper si Spring Boot 4
import com.junior.chat.model.ChatMessage;
import com.junior.chat.model.Envelope;
import com.junior.chat.model.MessageEntity;
import com.junior.chat.model.MessageStatus;
import com.junior.chat.service.MessageService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final MessageService messageService;

    public ChatHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    // enregistre l'utilisateur (déjà connecté) pour qu'il puisse recevoir des messages via WebSocket
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = userId(session);
        if (userId == null || userId.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("userId manquant"));
            return;
        }
        WebSocketSession decorated =
                new ConcurrentWebSocketSessionDecorator(session, 5000, 65536);
        sessions.put(userId, decorated);

        for (MessageEntity pending : messageService.pendingFor(userId)) {
            send(decorated, Envelope.Message.of(pending));
            messageService.markDelivered(pending.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage frame)
            throws Exception {
        Envelope.Message in = mapper.readValue(frame.getPayload(), Envelope.Message.class);
        String sender = userId(session);

        MessageEntity saved =
                messageService.persist(in.id(), sender, in.to(), in.content());
        send(sessions.get(sender), Envelope.Ack.of(saved.getId(), MessageStatus.SENT));

        WebSocketSession target = sessions.get(in.to());
        if (target != null && target.isOpen()) {
            send(target, Envelope.Message.of(saved));
            messageService.markDelivered(saved.getId());
            send(sessions.get(sender), Envelope.Ack.of(saved.getId(), MessageStatus.DELIVERED));
        }
    }

    private void send(WebSocketSession session, Envelope payload) {
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.warn("Échec d'envoi vers la session {}", session.getId(), e);
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
        // regarder l'évolution de session du début à la fin de cette fonction
        log.info("session.getUri() --> " + session.getUri());
        log.info("UriComponentsBuilder.fromUri(session.getUri()) --> " + UriComponentsBuilder.fromUri(session.getUri()));
        log.info("UriComponentsBuilder.fromUri(session.getUri()).build() --> " + UriComponentsBuilder.fromUri(session.getUri()).build());
        log.info("UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams() --> " + UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams());
        log.info("UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst(\"userId\") --> " + UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("userId"));
        return UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("userId");
    }
}