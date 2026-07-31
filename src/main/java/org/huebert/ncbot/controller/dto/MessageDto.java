package org.huebert.ncbot.controller.dto;

import org.huebert.ncbot.entity.ChatMessage;

import java.time.Instant;

public record MessageDto(Long id, String senderName, String content, Instant createdAt, String response) {

    public static MessageDto from(ChatMessage message) {
        return new MessageDto(message.getId(), message.getSenderName(), message.getContent(), message.getCreatedAt(), message.getResponse());
    }

}