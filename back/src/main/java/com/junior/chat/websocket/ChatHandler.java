package com.junior.chat.websocket;

import tools.jackson.databind.ObjectMapper; // tools.jackson.databind.ObjectMapper si Spring Boot 4
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
    // lors d'une commande websocat
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = userId(session);
        if (userId == null || userId.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("userId manquant"));
            return;
        }
        WebSocketSession decorated =
                new ConcurrentWebSocketSessionDecorator(session, 5000, 65536);
        sessions.put(userId, decorated);

        // 1. les messages qu'il n'a pas reçus
        for (MessageEntity pending : messageService.pendingFor(userId)) {
            send(decorated, Envelope.Message.of(pending));
            messageService.markDelivered(pending.getId());
            notifyDelivered(pending);
        }

        // 2. les accusés qu'il n'a pas reçus, pour ses propres messages
        for (MessageEntity delivered : messageService.deliveredNotNotified(userId)) {
            send(decorated, Envelope.Ack.of(delivered.getId(), MessageStatus.DELIVERED));
            messageService.markDeliveryNotified(delivered.getId());
        }
    }

    private void notifyDelivered(MessageEntity message) {
        WebSocketSession sender = sessions.get(message.getSender());
        if (sender != null && sender.isOpen()) {
            send(sender, Envelope.Ack.of(message.getId(), MessageStatus.DELIVERED));
            messageService.markDeliveryNotified(message.getId());
        }
    }

    @Override
    // gère les messages reçus par le serveur
    // le msg est d'abord sauvegardé dans la bdd, puis envoyé à la personne concernée
    // enfin, un accusé de réception est envoyé à l'expéditeur du msg pour lui indiquer que le msg a bien été reçu par le serveur
    protected void handleTextMessage(WebSocketSession session, TextMessage frame)
            throws Exception {
        // lire le msg reçu par le serveur et le convertir en objet Java
        Envelope.Message in = mapper.readValue(frame.getPayload(), Envelope.Message.class);
        // récupérer le nom de la personne ayant envoyée le sg au serveur
        String sender = userId(session);

        // sauvegarder le msg dans la bdd
        MessageEntity saved =
                messageService.persist(in.id(), sender, in.to(), in.content());
        // envoyer un accusé de réception au client qui a envoyé le msg
        send(sessions.get(sender), Envelope.Ack.of(saved.getId(), MessageStatus.SENT));

        WebSocketSession target = sessions.get(in.to());
        if (target != null && target.isOpen()) {
            send(target, Envelope.Message.of(saved));
            messageService.markDelivered(saved.getId());
            notifyDelivered(saved);
        }
    }

    // envoie le msg à la personne concernée
    // n'a aucune interraction avec la bdd, c'est juste une écriture dans le terminal
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
        return (String) session.getAttributes().get("userId");
    }
}