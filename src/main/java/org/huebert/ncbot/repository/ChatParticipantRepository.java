package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    @Query("""
            SELECT p
            FROM ChatParticipant p
            WHERE p.name = :name
            """)
    Optional<ChatParticipant> findParticipant(String name);

    @Query("""
            SELECT p
            FROM ChatParticipant p
            WHERE LOWER(p.name) LIKE LOWER(:name)
            ORDER BY p.name
            """)
    List<ChatParticipant> searchParticipants(String name);

    @Query("""
            SELECT p
            FROM ChatParticipant p
            ORDER BY p.lastSeen DESC
            """)
    List<ChatParticipant> findLastSeen();

}
