package org.huebert.ncbot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder(toBuilder = true)
@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_chat_item_channel_created", columnList = "chat_channel_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_channel_id", nullable = false)
    private Long chatChannelId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "response")
    private String response;

    @Column(name = "packet_hash")
    private String packetHash;

    @Column(name = "region")
    private String region;

    @Column(name = "scoped")
    private Boolean scoped;

    @Override
    public String toString() {
        return "ChatMessage{id=" + id +
                ", chatChannelId=" + chatChannelId +
                ", senderName='" + senderName + '\'' +
                ", content='" + truncate(content, 80) + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }

}
