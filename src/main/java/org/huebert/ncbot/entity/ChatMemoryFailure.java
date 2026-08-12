package org.huebert.ncbot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A record of a memory-synthesis partition that was skipped after repeated AI failures.
 * Written so a deterministically failing batch (e.g. provider content moderation on a
 * batch of messages) is observable instead of silently retried forever or silently dropped.
 */
@Builder(toBuilder = true)
@Entity
@Table(name = "chat_memory_failure", indexes = {
        @Index(name = "idx_chat_memory_failure_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_channel_id", nullable = false)
    private Long chatChannelId;

    @Column(name = "from_message_id")
    private Long fromMessageId;

    @Column(name = "to_message_id")
    private Long toMessageId;

    @Column(name = "error", length = 2000)
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}