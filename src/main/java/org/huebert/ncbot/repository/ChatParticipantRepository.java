package org.huebert.ncbot.repository;

import org.huebert.ncbot.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    @Query("""
            SELECT p
            FROM ChatParticipant p
            WHERE p.name = :name
            """)
    Optional<ChatParticipant> findParticipant(String name);

}
