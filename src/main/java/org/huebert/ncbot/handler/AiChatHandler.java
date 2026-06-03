package org.huebert.ncbot.handler;

import com.google.common.base.Utf8;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.AiMode;
import org.huebert.ncbot.config.ChannelCapabilities;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.service.TemplateService;
import org.huebert.ncbot.tool.ChatTool;
import org.huebert.ncbot.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AiChatHandler implements ChatHandler {

    private static final int ORDER = -100;

    private final ChatClient chatClient;
    private final NcbotProperties properties;
    private final TemplateService templateService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemory2Repository chatMemoryRepository;

    public AiChatHandler(
            ChatModel chatModel, WeatherTool weatherTool, ChatTool chatTool, NcbotProperties properties, TemplateService templateService, ChatMessageRepository chatMessageRepository, ChatMemory2Repository chatMemoryRepository
    ) {
        this.properties = properties;
        this.templateService = templateService;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(weatherTool, chatTool)
                .build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {
        log.debug("handle: request from {} in {}", request.senderName(), request.channelName());

        AiMode aiMode = properties.getChannelCapabilities(request)
                .map(ChannelCapabilities::ai)
                .orElse(AiMode.DISABLED);

        if (aiMode == AiMode.TAGGED) {
            boolean isTagged = request.messageText() != null && request.messageText().contains("@[" + properties.name() + "]");
            if (!isTagged) {
                log.debug("handle: ai mode is TAGGED, no tag in {}, skipping", request.channelName());
                return Optional.empty();
            }
            log.debug("handle: responding to tag in {}", request.channelName());
        } else if (aiMode == AiMode.DISABLED) {
            log.debug("handle: ai disabled in {}, skipping", request.channelName());
            return Optional.empty();
        }

        List<ChatMessage> messages = chatMessageRepository.findChannelMessages(chatChannel.getId(), chatChannel.getMemoryUpdatedAt(), Instant.now());
        List<ChatMemory> memories = chatMemoryRepository.findMemory(chatChannel.getId());
        log.debug("handle: loaded {} messages, {} memories for channel {}", messages.size(), memories.size(), chatChannel.getChannelName());

        String output = templateService.render("chat", Map.of(
                "memories", memories,
                "messages", messages,
                "request", request
        ));

        long start = System.currentTimeMillis();
        String response = chatClient.prompt()
                .system(properties.systemPrompt())
                .user(output)
                .messages()
                .call()
                .content();
        long elapsed = System.currentTimeMillis() - start;
        log.info("AI call completed in {} ms, response {} bytes for {} in {}", elapsed, Utf8.encodedLength(response), request.senderName(), request.channelName());

        if ("EMPTY".equalsIgnoreCase(Strings.trimToNull(response))) {
            log.debug("handle: AI returned empty response");
            return Optional.empty();
        }

        return condense(request, response);
    }

    private Optional<String> condense(ChatRequest request, String response) {
        log.debug("condense: {}", response);

        if (!properties.condense() || (Utf8.encodedLength(response) <= properties.maxReplyBytes())) {
            log.debug("condense result: no change {}", response);
            return Optional.of(response);
        }

        String output = templateService.render("condense", Map.of(
                "request", request,
                "response", response
        ));

        String condensed = chatClient.prompt()
                .system(properties.condensePrompt())
                .user(output)
                .call()
                .content();

        log.debug("condense result: consensed {}", condensed);
        return Optional.ofNullable(condensed);
    }

}
