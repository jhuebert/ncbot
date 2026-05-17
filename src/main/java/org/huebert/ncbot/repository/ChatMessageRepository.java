package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.chatChannelId = :chatChannelId AND m.createdAt > :start AND m.createdAt < :end
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findChannelMessages(Long chatChannelId, Instant start, Instant end);

}
