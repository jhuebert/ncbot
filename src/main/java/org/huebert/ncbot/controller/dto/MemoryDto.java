package org.huebert.ncbot.controller.dto;

import lombok.Builder;
import org.huebert.ncbot.entity.ChatMemory;

@Builder
public record MemoryDto(Long id, String key, String value) {

    public static MemoryDto from(ChatMemory m) {
        return MemoryDto.builder()
                .id(m.getId())
                .key(m.getKey())
                .value(m.getValue())
                .build();
    }

}
