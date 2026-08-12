package org.huebert.ncbot.controller.dto;

import lombok.Builder;
import org.huebert.ncbot.entity.ChatMemoryFailure;

import java.time.Instant;

@Builder
public record MemoryFailureDto(
        Long id,
        Long channelId,
        String channelName,
        Long fromMessageId,
        Long toMessageId,
        String error,
        Instant createdAt
) {

    public static MemoryFailureDto from(ChatMemoryFailure f, String channelName) {
        return MemoryFailureDto.builder()
                .id(f.getId())
                .channelId(f.getChatChannelId())
                .channelName(channelName)
                .fromMessageId(f.getFromMessageId())
                .toMessageId(f.getToMessageId())
                .error(f.getError())
                .createdAt(f.getCreatedAt())
                .build();
    }

}