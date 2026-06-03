package org.huebert.ncbot.controller.dto;

import org.huebert.ncbot.entity.ChatChannel;

public record ChannelDto(Long id, String channelKey, String channelName, Boolean isDm) {
    public static ChannelDto from(ChatChannel c) {
        return new ChannelDto(c.getId(), c.getChannelKey(), c.getChannelName(), c.getIsDm());
    }
}
