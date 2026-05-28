package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.util.Pair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.chatChannelId = :chatChannelId AND m.createdAt > :start AND m.createdAt < :end
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findChannelMessages(Long chatChannelId, Instant start, Instant end);

    @Query("""
            SELECT new org.huebert.ncbot.util.Pair(m.chatChannelId, MAX(m.createdAt))
            FROM ChatMessage m
            WHERE m.chatChannelId IN :chatChannelIds
            GROUP BY m.chatChannelId
            ORDER BY MAX(m.createdAt) DESC
            """)
    List<Pair<Long, Long>> findLastSeen(Set<Long> chatChannelIds);

    @Query(value = """
            SELECT * FROM chat_message
            WHERE chat_channel_id = :chatChannelId
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ChatMessage> findMessagesByChannelOrderByCreatedDesc(Long chatChannelId, int limit);

    @Query(value = """
            SELECT * FROM chat_message
            WHERE chat_channel_id = :chatChannelId AND created_at < :before
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ChatMessage> findMessagesByChannelBefore(Long chatChannelId, Instant before, int limit);

    @Query("""
            SELECT DISTINCT m.senderName
            FROM ChatMessage m
            WHERE m.chatChannelId = :chatChannelId
            ORDER BY m.senderName ASC
            """)
    List<String> findSenderNamesByChannel(Long chatChannelId);

}
