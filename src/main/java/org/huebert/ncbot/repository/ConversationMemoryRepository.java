package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ConversationMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ConversationMemoryRepository extends JpaRepository<ConversationMemory, Long> {

    @Query("""
            SELECT m
            FROM ConversationMemory m
            WHERE m.channelKey = :channelKey
            """)
    Optional<ConversationMemory> findChannelMemory(String channelKey);

    @Query("""
            SELECT m
            FROM ConversationMemory m
            WHERE m.senderKey = :senderKey
            """)
    Optional<ConversationMemory> findDmMemory(String senderKey);

}
