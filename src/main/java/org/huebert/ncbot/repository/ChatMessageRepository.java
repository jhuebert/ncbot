package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.util.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.chatChannelId = :chatChannelId
            ORDER BY m.createdAt DESC
            """)
    Page<ChatMessage> findMessagesByChannelOrderByCreatedDesc(Long chatChannelId, Pageable pageable);

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.chatChannelId = :chatChannelId AND m.createdAt < :before
            ORDER BY m.createdAt DESC
            """)
    Page<ChatMessage> findMessagesByChannelBefore(Long chatChannelId, Instant before, Pageable pageable);

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.chatChannelId = :chatChannelId AND m.createdAt > :after
            ORDER BY m.createdAt ASC
            """)
    Page<ChatMessage> findMessagesByChannelAfter(Long chatChannelId, Instant after, Pageable pageable);

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.chatChannelId = :chatChannelId AND m.createdAt < :before AND m.createdAt > :after
            ORDER BY m.createdAt DESC
            """)
    Page<ChatMessage> findMessagesByChannelBetween(Long chatChannelId, Instant before, Instant after, Pageable pageable);

    @Query("""
            SELECT DISTINCT m.senderName
            FROM ChatMessage m
            WHERE m.chatChannelId = :chatChannelId
            """)
    Set<String> findSenderNamesByChannel(Long chatChannelId);

}
