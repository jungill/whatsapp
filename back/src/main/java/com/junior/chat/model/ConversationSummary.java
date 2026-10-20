package com.junior.chat.model;

import java.time.Instant;

public record ConversationSummary(String peer, String lastMessage, Instant lastAt) {

    public static ConversationSummary of(MessageEntity m, String user) {
        String peer = m.getSender().equals(user) ? m.getRecipient() : m.getSender();
        return new ConversationSummary(peer, m.getContent(), m.getCreatedAt());
    }
}