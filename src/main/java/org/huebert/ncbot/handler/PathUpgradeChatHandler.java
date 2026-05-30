package org.huebert.ncbot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.service.TemplateService;
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

        boolean pathUpgrade = properties.getChannelProperties(request)
                .map(NcbotProperties.ChannelProperties::pathUpgrade)
                .orElse(false);
        if (!pathUpgrade) {
            return Optional.empty();
        }

        boolean usingOneByte = isUsingOneBytePath(request);
        if (!usingOneByte) {
            return Optional.empty();
        }

        ChatParticipant participant = chatParticipantRepository.findParticipant(request.senderName()).orElse(null);
        if (participant == null) {
            return Optional.empty();
        }

        int cooldownMinutes = properties.pathUpgradeCooldownMinutes();
        Instant notifiedAt = participant.getPathUpgradeNotifiedAt();
        if (notifiedAt != null && notifiedAt.plus(Duration.ofMinutes(cooldownMinutes)).isAfter(Instant.now())) {
            return Optional.empty();
        }

        log.debug("notifying {} to upgrade path hash", request.senderName());
        participant.setPathUpgradeNotifiedAt(Instant.now());
        chatParticipantRepository.save(participant);

        return Optional.of(templateService.render("path-upgrade", Map.of(
                "request", request
        )));
    }

    private boolean isUsingOneBytePath(ChatRequest request) {
        Integer pathBytesPerHop = request.pathBytesPerHop();
        return pathBytesPerHop != null && pathBytesPerHop == 1;
    }
}
