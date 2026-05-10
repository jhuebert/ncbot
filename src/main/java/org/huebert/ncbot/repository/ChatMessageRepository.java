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
            WHERE m.channelKey = :channelKey AND m.createdAt > :since
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findLatestChannelMessages(String channelKey, Instant since);

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.isDm IS TRUE AND m.senderKey = :senderKey AND m.createdAt > :since
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findLatestDms(String senderKey, Instant since);

    long countByChannelKey(String channelKey);

    @Query("SELECT DISTINCT m.channelKey FROM ChatMessage m WHERE m.channelKey IS NOT NULL")
    List<String> findDistinctChannelKeys();

}
