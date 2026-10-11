package com.junior.chat.repository;

import com.junior.chat.model.MessageEntity;
import com.junior.chat.model.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    List<MessageEntity> findByRecipientAndStatusOrderByCreatedAtAsc(
            String recipient, MessageStatus status);

    List<MessageEntity> findBySenderAndStatusOrderByCreatedAtAsc(
        String sender, MessageStatus status);

@Query("""
        select m from MessageEntity m
        where (m.sender = :a and m.recipient = :b)
                or (m.sender = :b and m.recipient = :a)
        order by m.createdAt asc
        """)
List<MessageEntity> findConversation(@Param("a") String a, @Param("b") String b);
}