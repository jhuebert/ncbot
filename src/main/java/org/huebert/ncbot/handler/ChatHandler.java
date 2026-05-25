package org.huebert.ncbot.handler;

import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ChatHandler extends Comparable<ChatHandler> {

    int getOrder();

    Optional<String> handle(ChatChannel chatChannel, ChatRequest request);

    @Override
    default int compareTo(@NotNull ChatHandler o) {
        return -Integer.compare(getOrder(), o.getOrder());
    }
}
