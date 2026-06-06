package org.huebert.ncbot.handler;

import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public interface ChatHandler extends Comparable<ChatHandler> {

    String DO_NOT_RESPOND = "__DO_NOT_RESPOND__";

    int getOrder();

    Optional<String> handle(ChatChannel chatChannel, ChatRequest request);

    @Override
    default int compareTo(@NonNull ChatHandler o) {
        return -Integer.compare(getOrder(), o.getOrder());
    }

}
