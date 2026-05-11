package org.huebert.ncbot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder(toBuilder = true)
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_channel_created", columnList = "channel_key, created_at"),
        @Index(name = "idx_sender_created", columnList = "sender_key, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_key", length = 64)
    private String senderKey;

    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "is_dm")
    private Boolean isDm;

    @Column(name = "channel_key", length = 32)
    private String channelKey;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "sender_timestamp")
    private Long senderTimestamp;

    @Column(name = "path")
    private String path;

    @Column(name = "is_outgoing")
    private Boolean isOutgoing;

    @Column(name = "path_bytes_per_hop")
    private Integer pathBytesPerHop;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
