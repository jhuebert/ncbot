package org.huebert.ncbot.handler;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.AiMode;
import org.huebert.ncbot.config.ChannelCapabilities;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.service.AiService;
import org.huebert.ncbot.service.ConfigService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class AiChatHandler implements ChatHandler {

    private static final int ORDER = -100;

    private final ConfigService configService;
    private final AiService aiService;

    public AiChatHandler(ConfigService configService, AiService aiService) {
        this.configService = configService;
        this.aiService = aiService;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    @DebugLog
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {

        AiMode aiMode = configService.getChannelCapabilities(request)
                .map(ChannelCapabilities::ai)
                .orElse(AiMode.DISABLED);

        if (aiMode == AiMode.TAGGED) {
            boolean isTagged = request.messageText() != null && request.messageText().contains("@[" + configService.name() + "]");
            if (!isTagged) {
                log.debug("handle: ai mode is TAGGED, no tag in {}, skipping", request.channelName());
                return Optional.empty();
            }
            log.debug("handle: responding to tag in {}", request.channelName());
        } else if (aiMode == AiMode.DISABLED) {
            log.debug("handle: ai disabled in {}, skipping", request.channelName());
            return Optional.empty();
        }

        return aiService.respondToUser(chatChannel, request);
    }

}
