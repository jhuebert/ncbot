package org.huebert.ncbot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.util.PatternUtil;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Blocks messages from malicious users/paths based on regex allow/block lists.
 * <p>
 * Runs before all other handlers.
 * Short-circuits the chain for blocked users/paths.
 * <p>
 * Precedence: allow always beats block. If a user/path matches an allow pattern,
 * they are allowed regardless of block patterns.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockingChatHandler implements ChatHandler {

    private static final int ORDER = 200;

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
        if (PatternUtil.matches(senderName, properties.allowUser())) {
            log.debug("allowed user {} (matched allow pattern {})", senderName, properties.allowUser());
            return null;
        }

        if (PatternUtil.matches(senderName, properties.blockUser())) {
            return "user:" + senderName;
        }

        String path = Strings.join(request.getPathItems(), ',');
        if (PatternUtil.matches(path, properties.allowPath())) {
            log.debug("allowed path for {} (matched allow pattern {})", senderName, properties.allowPath());
            return null;
        }

        if (PatternUtil.matches(path, properties.blockPath())) {
            return "path:" + properties.blockPath();
        }

        // DMs skip channel-level blocking
        if (!request.isDm()) {
            String channelName = request.channelName();
            if (PatternUtil.matches(channelName, properties.allowChannel())) {
                log.debug("allowed channel {} (matched allow pattern {})", channelName, properties.allowChannel());
                return null;
            }

            if (PatternUtil.matches(channelName, properties.blockChannel())) {
                return "channel:" + channelName;
            }
        }

        log.debug("user {} does not match any allow or block patterns", senderName);
        return null;
    }

}
