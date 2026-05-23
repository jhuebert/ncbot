package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMemory2Repository extends JpaRepository<ChatMemory, Long> {

    @Query("""
            SELECT m
            FROM ChatMemory m
            WHERE m.chatChannelId = :chatChannelId OR m.chatChannelId IS NULL
            ORDER BY m.key ASC
            """)
    List<ChatMemory> findMemory(Long chatChannelId);

}
