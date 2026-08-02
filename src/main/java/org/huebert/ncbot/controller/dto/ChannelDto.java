package org.huebert.ncbot.controller.dto;

import lombok.Builder;
import org.huebert.ncbot.entity.ChatChannel;

import java.time.Instant;

@Builder
public record ChannelDto(
        Long id,
        String channelKey,
        String channelName,
        Boolean isDm,
        Instant lastMessageAt
) {

    public static ChannelDto from(ChatChannel c, Instant lastMessageAt) {
        return ChannelDto.builder()
                .id(c.getId())
                .channelKey(c.getChannelKey())
                .channelName(c.getChannelName())
                .isDm(c.getIsDm())
                .lastMessageAt(lastMessageAt)
                .build();
    }

}
