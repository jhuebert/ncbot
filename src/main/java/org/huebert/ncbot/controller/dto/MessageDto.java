package org.huebert.ncbot.controller.dto;

import org.huebert.ncbot.entity.ChatMessage;

public record MessageDto(Long id, String senderName, String content, String createdAt) {
    public static MessageDto from(ChatMessage m) {
        return new MessageDto(m.getId(), m.getSenderName(), m.getContent(), m.getCreatedAt().toString());
    }
}
