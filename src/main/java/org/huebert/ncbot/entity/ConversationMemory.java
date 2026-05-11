package org.huebert.ncbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@Entity
@Table(name = "conversation_memory", indexes = {
        @Index(name = "idx_channel", columnList = "channel_key"),
        @Index(name = "idx_dm_sender", columnList = "sender_key")
})
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_key", length = 64)
    private String senderKey;

    @Column(name = "channel_key", length = 32)
    private String channelKey;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @NonNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
