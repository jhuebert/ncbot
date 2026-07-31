package org.huebert.ncbot.controller.dto;

import org.huebert.ncbot.entity.ChatMessage;
import org.springframework.data.domain.Page;

import java.util.List;

public record MessagesResponse(
        long channelId,
        String channelName,
        List<MessageDto> messages,
        int totalPages,
        int currentPage,
        long totalElements
) {

    public static MessagesResponse fromPage(long channelId, String channelName, Page<ChatMessage> page) {
        return new MessagesResponse(
                channelId,
                channelName,
                page.getContent().stream().map(MessageDto::from).toList(),
                page.getTotalPages(),
                page.getNumber(),
                page.getTotalElements()
        );
    }

}