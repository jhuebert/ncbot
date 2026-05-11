package org.huebert.ncbot.util;

import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatMessage;

public class MessageFormatter {

    private static final String MESSAGE_FORMAT = "%s: %s";

    public static String buildUserMessage(ChatRequest request) {
        return String.format(MESSAGE_FORMAT, request.senderName(), request.messageText());
    }

    public static String buildUserMessage(ChatMessage message) {
        return String.format(MESSAGE_FORMAT, message.getSenderName(), message.getMessageText());
    }

}
