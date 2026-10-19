package com.junior.chat.repository;

import com.junior.chat.model.ConversationCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ConversationCounterRepository
        extends JpaRepository<ConversationCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConversationCounter> findById(String conversationId);
}