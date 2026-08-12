package org.huebert.ncbot.handler;

import com.google.common.base.Utf8;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.AiMode;
import org.huebert.ncbot.config.ChannelCapabilities;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.PromptMessage;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.service.ConfigService;
import org.huebert.ncbot.service.TemplateService;
import org.huebert.ncbot.tool.MemoryTool;
import org.huebert.ncbot.tool.WeatherTool;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AiChatHandler implements ChatHandler {

    private static final int ORDER = -100;

    private final ChatClient chatClient;
    private final NcbotProperties properties;
    private final ConfigService configService;
    private final TemplateService templateService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemory2Repository chatMemoryRepository;

    public AiChatHandler(
            ChatModel chatModel,
            WeatherTool weatherTool,
            MemoryTool memoryTool,
            NcbotProperties properties,
            ConfigService configService,
            TemplateService templateService,
            ChatMessageRepository chatMessageRepository,
            ChatMemory2Repository chatMemoryRepository
    ) {
        this.properties = properties;
        this.configService = configService;
        this.templateService = templateService;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatClient = ChatClient.builder(chatModel)
                // Cap output tokens per round-trip on the reply path (chat response,
                // tool-call arguments, and condense). Replies are ≤128 bytes, so 256
                // is generous; the cap mainly bounds rambling and tool-call loops.
                .defaultOptions(OpenAiChatOptions.builder().maxTokens(configService.maxReplyTokens()))
                .defaultTools(weatherTool, memoryTool)
                .build();
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

        Pageable pageable = PageRequest.of(0, configService.maxChatHistory());

        Instant start = chatChannel.getMemoryUpdatedAt();
        Instant end = Instant.now();
        Instant startMax = end.minus(properties.memoryUpdatePeriod());
        if (startMax.isAfter(start)) {
            start = startMax;
        }

        List<ChatMessage> messages = chatMessageRepository.findChannelMessages(chatChannel.getId(), start, end, pageable).reversed();
        List<ChatMemory> memories = configService.useMemory()
                ? chatMemoryRepository.findMemory(chatChannel.getId())
                : List.of();
        log.debug("handle: loaded {} messages, {} memories for channel {}", messages.size(), memories.size(), chatChannel.getChannelName());

        List<PromptMessage> promptMessages = new ArrayList<>(messages.stream().map(PromptMessage::from).toList());
        promptMessages.add(PromptMessage.now(request.senderName(), request.messageText()));

        String output = templateService.render("chat", Map.of(
                "channelId", chatChannel.getId(),
                "memories", memories,
                "messages", promptMessages,
                "request", request
        ));

        String response = chatClient.prompt()
                .system(configService.systemPrompt())
                .user(output)
                .call()
                .content();

        if ("EMPTY".equalsIgnoreCase(Strings.trimToNull(response))) {
            log.debug("handle: AI returned empty response");
            return Optional.empty();
        }

        return condense(request, response);
    }

    private Optional<String> condense(ChatRequest request, String response) {
        log.debug("condense: {}", response);

        if (!configService.condense() || (Utf8.encodedLength(response) <= configService.maxReplyBytes())) {
            log.debug("condense result: no change {}", response);
            return Optional.of(response);
        }

        String output = templateService.render("condense", Map.of(
                "request", request,
                "response", response
        ));

        String condensed = chatClient.prompt()
                .system(configService.condensePrompt())
                .user(output)
                .call()
                .content();

        log.debug("condense result: consensed {}", condensed);
        return Optional.ofNullable(condensed);
    }

}
