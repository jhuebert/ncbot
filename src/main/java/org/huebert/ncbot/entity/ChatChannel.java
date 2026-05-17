package org.huebert.ncbot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder(toBuilder = true)
@Data
@Entity
@Table(name = "chat_channel", indexes = {
        @Index(name = "idx_chat_channel_dm_channel", columnList = "is_dm, channel_key")
})
@NoArgsConstructor
@AllArgsConstructor
public class ChatChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_key", nullable = false)
    private String channelKey;

    @Column(name = "is_dm", nullable = false)
    private Boolean isDm;

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    @Column(name = "memory_updated_at", nullable = false)
    private Instant memoryUpdatedAt;

}
