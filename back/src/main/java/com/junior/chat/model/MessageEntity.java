package com.junior.chat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_recipient_status", columnList = "recipient, status"),
        @Index(name = "idx_conversation_sequence", columnList = "conversationId, sequence")
})
public class MessageEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private long sequence;

    protected MessageEntity() {}

    public MessageEntity(String id, String sender, String recipient,
                         String content, String conversationId, long sequence) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.conversationId = conversationId;
        this.sequence = sequence;
        this.status = MessageStatus.SENT;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getContent() { return content; }
    public MessageStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getConversationId() { return conversationId; }
    public long getSequence() { return sequence; }

    public void markDelivered() { this.status = MessageStatus.DELIVERED; }
    public void markDeliveryNotified() { this.status = MessageStatus.DELIVERY_NOTIFIED; }
}