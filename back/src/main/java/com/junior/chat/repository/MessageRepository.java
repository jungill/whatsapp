package com.junior.chat.repository;

import com.junior.chat.model.MessageEntity;
import com.junior.chat.model.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    List<MessageEntity> findByRecipientAndStatusOrderByCreatedAtAsc(
            String recipient, MessageStatus status);
}