package org.huebert.ncbot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.service.ConfigService;
import org.huebert.ncbot.util.DebugLog;
import org.huebert.ncbot.util.PatternUtil;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Blocks messages from malicious users/paths based on regex allow/block lists
 * and per-participant block flags.
 * <p>
 * Runs before all other handlers.
 * Short-circuits the chain for blocked users/paths.
 * <p>
 * Precedence: allow always beats block. If a user/path matches an allow pattern,
 * they are allowed regardless of block patterns. A participant explicitly blocked
 * via the admin API behaves like a {@code block-user} regex match.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockingChatHandler implements ChatHandler {

    private static final int ORDER = 200;

    private final ConfigService configService;
    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    @DebugLog
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
        if (PatternUtil.matches(senderName, configService.allowUser())) {
            log.debug("allowed user {} (matched allow pattern {})", senderName, configService.allowUser());
            return null;
        }

        if (isBlockedParticipant(senderName)) {
            return "participant:" + senderName;
        }

        if (PatternUtil.matches(senderName, configService.blockUser())) {
            return "user:" + senderName;
        }

        String path = Strings.join(request.getPathItems(), ',');
        if (PatternUtil.matches(path, configService.allowPath())) {
            log.debug("allowed path for {} (matched allow pattern {})", senderName, configService.allowPath());
            return null;
        }

        if (PatternUtil.matches(path, configService.blockPath())) {
            return "path:" + configService.blockPath();
        }

        // DMs skip channel-level blocking
        if (!request.isDm()) {
            String channelName = request.channelName();
            if (PatternUtil.matches(channelName, configService.allowChannel())) {
                log.debug("allowed channel {} (matched allow pattern {})", channelName, configService.allowChannel());
                return null;
            }

            if (PatternUtil.matches(channelName, configService.blockChannel())) {
                return "channel:" + channelName;
            }
        }

        log.debug("user {} does not match any allow or block patterns", senderName);
        return null;
    }

    private boolean isBlockedParticipant(String senderName) {
        return chatParticipantRepository.findParticipant(senderName)
                .map(ChatParticipant::isBlocked)
                .orElse(false);
    }

}
