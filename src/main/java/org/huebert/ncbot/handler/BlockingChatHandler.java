package org.huebert.ncbot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Blocks messages from malicious users/paths based on regex allow/block lists.
 * <p>
 * Runs after welcome and path-upgrade handlers but before command and AI handlers.
 * Short-circuits the chain for blocked users/paths.
 * <p>
 * Precedence: allow always beats block. If a user/path matches an allow pattern,
 * they are allowed regardless of block patterns.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockingChatHandler implements ChatHandler {

    private static final int ORDER = 70;

    private final NcbotProperties properties;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {
        String reason = shouldBlock(request);
        if (reason != null) {
            log.warn("blocked {} in {} (reason: {})", request.senderName(), request.channelName(), reason);
            return Optional.of(DO_NOT_RESPOND);
        }
        return Optional.empty();
    }

    private String shouldBlock(ChatRequest request) {

        String senderName = request.senderName();
        if (matches(senderName, properties.allowUserPattern())) {
            log.debug("allowed user {} (matched allow pattern {})", senderName, properties.allowUserPattern());
            return null;
        }

        if (matches(senderName, properties.blockUserPattern())) {
            return "user:" + senderName;
        }

        String path = Strings.join(request.getPathItems(), ',');
        if (matches(path, properties.allowPathPattern())) {
            log.debug("allowed path for {} (matched allow pattern {})", senderName, properties.allowPathPattern());
            return null;
        }

        if (matches(path, properties.blockPathPattern())) {
            return "path:" + properties.blockPathPattern();
        }

        log.debug("user {} does not match any allow or block patterns", senderName);
        return null;
    }

    private boolean matches(String value, String pattern) {
        if (Strings.isBlank(pattern)) {
            return false;
        }
        return value.matches(pattern);
    }

}
