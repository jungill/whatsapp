package com.junior.chat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages", indexes = @Index(name = "idx_recipient_status",
        columnList = "recipient, status"))
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

    protected MessageEntity() {}

    public MessageEntity(String id, String sender, String recipient, String content) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.status = MessageStatus.SENT;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getContent() { return content; }
    public MessageStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void markDelivered() { this.status = MessageStatus.DELIVERED; }
}