package org.huebert.ncbot.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.springframework.stereotype.Component;

import java.util.List;
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
public class BlockingChatHandler implements ChatHandler {

    private static final int ORDER = 70;

    private final NcbotProperties properties;

    public BlockingChatHandler(NcbotProperties properties) {
        this.properties = properties;
    }

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

        String reason;

        // 1. Check allowUserPatterns first — if any match, allow immediately
        String senderName = request.senderName();
        reason = findMatchingPattern(senderName, properties.allowUserPatterns());
        if (reason != null) {
            log.debug("allowed user {} (matched allow pattern {})", senderName, reason);
            return null;
        }

        // 2. Check blockUserPatterns — if any match, block
        reason = findMatchingPattern(senderName, properties.blockUserPatterns());
        if (reason != null) {
            return "user:" + reason;
        }

        // Check allowPathPatterns first
        String path = Strings.join(request.getPathItems(), ',');
        reason = findMatchingPattern(path, properties.allowPathPatterns());
        if (reason != null) {
            log.debug("allowed path for {} (matched allow pattern {})", senderName, reason);
            return null;
        }

        // Check blockPathPatterns
        reason = findMatchingPattern(path, properties.blockPathPatterns());
        if (reason != null) {
            return "path:" + reason;
        }

        return null;
    }

    private String findMatchingPattern(String value, List<String> patterns) {
        if (patterns == null) {
            return null;
        }
        return patterns.stream()
                .filter(value::matches)
                .findAny()
                .orElse(null);
    }

}
