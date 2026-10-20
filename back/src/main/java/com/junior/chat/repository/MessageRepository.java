package com.junior.chat.repository;

import com.junior.chat.model.MessageEntity;
import com.junior.chat.model.MessageStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    List<MessageEntity> findByRecipientAndStatusOrderByCreatedAtAsc(
            String recipient, MessageStatus status);

    List<MessageEntity> findBySenderAndStatusOrderByCreatedAtAsc(
        String sender, MessageStatus status);

        List<MessageEntity> findByConversationIdOrderBySequenceDesc(
                String conversationId, Pageable pageable);

        List<MessageEntity> findByConversationIdAndSequenceLessThanOrderBySequenceDesc(
                String conversationId, long before, Pageable pageable);

        @Query("""
        select m from MessageEntity m
        where (m.sender = :user or m.recipient = :user)
          and m.sequence = (
              select max(m2.sequence) from MessageEntity m2
              where m2.conversationId = m.conversationId)
        order by m.createdAt desc
        """)
        List<MessageEntity> findLastMessagesOf(@Param("user") String user);
}