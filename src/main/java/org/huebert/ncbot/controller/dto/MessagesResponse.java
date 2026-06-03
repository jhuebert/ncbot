package org.huebert.ncbot.controller.dto;

import java.util.List;

public record MessagesResponse(Long channelId, String channelName, List<MessageDto> messages,
                                int totalPages, int currentPage, long totalElements) {
    public MessagesResponse(Long channelId, String channelName, List<MessageDto> messages,
                            int totalPages, int currentPage, long totalElements) {
        this.channelId = channelId;
        this.channelName = channelName != null ? channelName : (channelId != null ? "Channel #" + channelId : null);
        this.messages = messages;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.totalElements = totalElements;
    }
}
