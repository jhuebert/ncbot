package org.huebert.ncbot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.ChannelCapabilities;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.service.TemplateService;
import org.huebert.ncbot.util.Truncate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class PathUpgradeChatHandler implements ChatHandler {

    private static final int ORDER = 75;

    private final ChatParticipantRepository chatParticipantRepository;
    private final NcbotProperties properties;
    private final TemplateService templateService;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {
        log.debug("handle: request from {} in {}", request.senderName(), request.channelName());

        boolean pathUpgrade = properties.getChannelCapabilities(request)
                .map(ChannelCapabilities::pathUpgrade)
                .orElse(false);
        if (!pathUpgrade) {
            log.debug("handle: pathUpgrade disabled for {}, skipping", request.channelName());
            return Optional.empty();
        }

        boolean usingOneByte = Truncate.isUsingOneBytePath(request);
        if (!usingOneByte) {
            log.debug("handle: {} not using 1-byte path, skipping", request.senderName());
            return Optional.empty();
        }

        ChatParticipant participant = chatParticipantRepository.findParticipant(request.senderName()).orElse(null);
        if (participant == null) {
            log.debug("handle: no participant record for {}, skipping", request.senderName());
            return Optional.empty();
        }

        int cooldownMinutes = properties.pathUpgradeCooldownMinutes();
        Instant notifiedAt = participant.getPathUpgradeNotifiedAt();
        if (notifiedAt != null && notifiedAt.plus(Duration.ofMinutes(cooldownMinutes)).isAfter(Instant.now())) {
            log.debug("handle: {} in cooldown (notified at {})", request.senderName(), notifiedAt);
            return Optional.empty();
        }

        log.info("path upgrade notification sent to {} in {}", request.senderName(), request.channelName());
        participant.setPathUpgradeNotifiedAt(Instant.now());
        chatParticipantRepository.save(participant);

        String response = templateService.render("path-upgrade", Map.of(
                "request", request
        ));
        return Optional.of(response);
    }
}
