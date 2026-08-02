package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatMemory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT m
            FROM ChatMemory m
            WHERE m.chatChannelId = :chatChannelId
            ORDER BY m.key ASC
            """)
    Page<ChatMemory> findChannelMemory(Long chatChannelId, Pageable pageable);

    @Query("""
            SELECT m
            FROM ChatMemory m
            WHERE m.chatChannelId IS NULL
            ORDER BY m.key ASC
            """)
    Page<ChatMemory> findGlobalMemory(Pageable pageable);

    @Query("""
            SELECT m
            FROM ChatMemory m
            WHERE m.chatChannelId = :chatChannelId
              AND (LOWER(m.key) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(m.value) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY m.key ASC
            """)
    Page<ChatMemory> searchChannelMemory(Long chatChannelId, String query, Pageable pageable);

    @Query("""
            SELECT m
            FROM ChatMemory m
            WHERE m.chatChannelId IS NULL
              AND (LOWER(m.key) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(m.value) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY m.key ASC
            """)
    Page<ChatMemory> searchGlobalMemory(String query, Pageable pageable);

    void deleteByChatChannelId(Long chatChannelId);

}
