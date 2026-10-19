package com.junior.chat.model;

import jakarta.persistence.*;

@Entity
@Table(name = "conversation_counters")
public class ConversationCounter {

    @Id
    private String conversationId;

    @Column(nullable = false)
    private long lastSequence;

    protected ConversationCounter() {}

    public ConversationCounter(String conversationId) {
        this.conversationId = conversationId;
        this.lastSequence = 0;
    }

    public long next() {
        return ++this.lastSequence;
    }
}