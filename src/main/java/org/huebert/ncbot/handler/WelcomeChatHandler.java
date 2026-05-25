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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class WelcomeChatHandler implements ChatHandler {

    private static final int ORDER = 100;

    private final ChatParticipantRepository chatParticipantRepository;
    private final NcbotProperties ncbotProperties;
    private final TemplateService templateService;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {

        ChatParticipant participant = chatParticipantRepository.findParticipant(request.senderName()).orElse(null);
        if (participant != null) {
            participant.setLastSeen(Instant.now());
            chatParticipantRepository.save(participant);
            return Optional.empty();
        }

        log.debug("new participant {}", request.senderName());
        chatParticipantRepository.save(ChatParticipant.builder()
                .name(request.senderName())
                .lastSeen(Instant.now())
                .build());

        boolean welcome = ncbotProperties.getChannelProperties(request)
                .map(NcbotProperties.ChannelProperties::welcome)
                .orElse(false);

        if (welcome) {
            log.debug("sending welcome to {}", request.senderName());
            return Optional.of(templateService.render("welcome", Map.of(
                    "request", request,
                    "welcomeContent", ncbotProperties.welcomeContent()
            )));
        }

        return Optional.empty();
    }

}
