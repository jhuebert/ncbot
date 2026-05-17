package org.huebert.ncbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@Entity
@Table(name = "chat_memory", indexes = {
        @Index(name = "idx_chat_memory_channel_key", columnList = "chat_channel_id, key_text")
})
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_channel_id", nullable = false)
    private Long chatChannelId;

    @Column(name = "key_text", nullable = false)
    private String key;

    @Column(name = "value_text", nullable = false)
    private String value;

}
