package org.huebert.ncbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder(toBuilder = true)
@Data
@Entity
@Table(name = "chat_participant", indexes = {
        @Index(name = "idx_chat_participant_name", columnList = "name")
})
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "path_upgrade_notified_at")
    private Instant pathUpgradeNotifiedAt;

}
