package org.huebert.ncbot.controller.dto;

import org.huebert.ncbot.entity.ChatMemory;

public record MemoryDto(Long id, Long channelId, String key, String value) {
    public static MemoryDto from(ChatMemory m) {
        return new MemoryDto(m.getId(), m.getChatChannelId(), m.getKey(), m.getValue());
    }
}
