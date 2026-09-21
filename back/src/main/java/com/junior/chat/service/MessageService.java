package com.junior.chat.service;

import com.junior.chat.model.*;
import com.junior.chat.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository repository;

    public MessageService(MessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MessageEntity persist(String id, String sender, String recipient, String content) {
        return repository.findById(id)
                .orElseGet(() -> repository.save(
                        new MessageEntity(id, sender, recipient, content)));
    }

    @Transactional
    public void markDelivered(String messageId) {
        repository.findById(messageId).ifPresent(MessageEntity::markDelivered);
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> pendingFor(String userId) {
        return repository.findByRecipientAndStatusOrderByCreatedAtAsc(
                userId, MessageStatus.SENT);
    }
}