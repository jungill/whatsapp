package com.junior.chat.service;

import com.junior.chat.model.*;
import com.junior.chat.repository.ConversationCounterRepository;
import com.junior.chat.repository.MessageRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository repository;
    private final ConversationCounterRepository counters;

    public MessageService(MessageRepository repository, ConversationCounterRepository counters) {
        this.repository = repository;
        this.counters = counters;
    }

    @Transactional
    public MessageEntity persist(String id, String sender, String recipient, String content) {
        return repository.findById(id).orElseGet(() -> {
            String conversationId = ConversationId.of(sender, recipient);
            ConversationCounter counter = counters.findById(conversationId)
                    .orElseGet(() -> counters.save(new ConversationCounter(conversationId)));
            long sequence = counter.next();
            return repository.save(new MessageEntity(
                    id, sender, recipient, content, conversationId, sequence));
        });
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

    @Transactional(readOnly = true)
    public List<MessageEntity> deliveredNotNotified(String sender) {
        return repository.findBySenderAndStatusOrderByCreatedAtAsc(
                sender, MessageStatus.DELIVERED);
    }

    @Transactional
    public void markDeliveryNotified(String messageId) {
        repository.findById(messageId).ifPresent(MessageEntity::markDeliveryNotified);
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> conversation(String a, String b, Long before, int limit) {
        String id = ConversationId.of(a, b);
        PageRequest page = PageRequest.of(0, limit);
        List<MessageEntity> result = (before == null)
                ? repository.findByConversationIdOrderBySequenceDesc(id, page)
                : repository.findByConversationIdAndSequenceLessThanOrderBySequenceDesc(id, before, page);
        return result.reversed();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> conversationsOf(String user) {
        return repository.findLastMessagesOf(user).stream()
                .map(m -> ConversationSummary.of(m, user))
                .toList();
    }
}