package com.junior.chat.model;
import java.time.Instant;

public sealed interface Envelope {

    record Message(String type, String id, String from, String to, String content, long sequence, Instant createdAt) implements Envelope {
        public static Message of(MessageEntity e) {
            return new Message("message", e.getId(), e.getSender(),
                    e.getRecipient(), e.getContent(), e.getSequence(), e.getCreatedAt());
        }
    }

    record Ack(String type, String messageId, MessageStatus status) implements Envelope {
        public static Ack of(String messageId, MessageStatus status) {
            return new Ack("ack", messageId, status);
        }
    }
}