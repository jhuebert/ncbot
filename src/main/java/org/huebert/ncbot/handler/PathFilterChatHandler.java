package org.huebert.ncbot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.util.DebugLog;
import org.huebert.ncbot.util.PathUtil;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Blocks messages arriving on 1-byte paths from reaching command and AI handlers.
 * <p>
 * Welcome and path-upgrade handlers (which run before this filter) can still respond.
 * Command and AI handlers (which run after) will never see 1-byte path messages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PathFilterChatHandler implements ChatHandler {

    private static final int ORDER = 60;

    private final NcbotProperties properties;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    @DebugLog
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {
        if (!properties.allowOneBytePaths() && PathUtil.isUsingOneBytePath(request)) {
            return Optional.of(DO_NOT_RESPOND);
        }
        return Optional.empty();
    }

}
