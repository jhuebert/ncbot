package org.huebert.ncbot.repository;

import java.time.Instant;
import java.util.List;

import org.huebert.ncbot.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {


    List<ChatMessage> findAllByChannelKeyOrderByCreatedAtDesc(String channelKey, Pageable pageable);

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.channelKey = :channelKey AND m.createdAt > :since
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findLatestChannelMessages(String channelKey, Instant since);

    List<ChatMessage> findAllByIsDmAndSenderKeyOrderByCreatedAtDesc(Boolean isDm, String senderKey, Pageable pageable);

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.isDm IS TRUE AND m.senderKey = :senderKey AND m.createdAt > :since
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findLatestDms(String senderKey, Instant since);

    long countByChannelKey(String channelKey);

    long countByIsDmAndSenderKey(Boolean isDm, String senderKey);

    @Query("SELECT DISTINCT m.channelKey FROM ChatMessage m WHERE m.channelKey IS NOT NULL")
    List<String> findDistinctChannelKeys();

    @Query("SELECT DISTINCT m.senderKey FROM ChatMessage m WHERE m.isDm = true AND m.senderKey IS NOT NULL")
    List<String> findDistinctDmSenderKeys();

}
