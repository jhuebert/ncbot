package org.huebert.ncbot.handler;

import org.huebert.ncbot.dto.ChatRequest;

import java.util.Optional;
import java.util.Set;

public interface CommandChatHandler extends ChatHandler {

    int ORDER = 50;

    @Override
    default int getOrder() {
        return ORDER;
    }

    default boolean matches(ChatRequest request, Set<String> commands) {
        String message = Optional.ofNullable(request.messageText())
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse("");
        return commands.contains(message);
    }

}
