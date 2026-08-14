package org.huebert.ncbot.service;

import com.google.common.base.Utf8;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.PromptMessage;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.tool.ByteLengthTool;
import org.huebert.ncbot.tool.HistoryTool;
import org.huebert.ncbot.tool.MemoryTool;
import org.huebert.ncbot.tool.ParticipantTool;
import org.huebert.ncbot.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates an AI reply for a channel. Both the normal chat path
 * ({@link org.huebert.ncbot.handler.AiChatHandler}, which is gated on the
 * channel's AI mode) and commands that must reply via AI regardless of the
 * channel's AI mode (e.g. {@code wx}/{@code weather}) delegate here.
 */
@Slf4j
@Service
public class AiService {

    private final ChatClient chatClient;
    private final NcbotProperties properties;
    private final ConfigService configService;
    private final TemplateService templateService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemory2Repository chatMemoryRepository;

    public AiService(
            ChatModel chatModel,
            WeatherTool weatherTool,
            MemoryTool memoryTool,
            ByteLengthTool byteLengthTool,
            HistoryTool historyTool,
            ParticipantTool participantTool,
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
                .defaultTools(weatherTool, memoryTool, byteLengthTool, historyTool, participantTool)
                .build();
    }

    /**
     * Produce an AI reply to the user's request, bypassing any channel AI-mode gating.
     * Builds the chat prompt from recent channel history + memories and the user's
     * message, then condenses any oversized response.
     *
     * @param chatChannel the channel the reply is destined for
     * @param request     the original chat request
     * @return the (possibly condensed) reply, or empty if the model returned "EMPTY"
     */
    public Optional<String> respondToUser(ChatChannel chatChannel, ChatRequest request) {
        return respond(chatChannel, request, request.messageText());
    }

    /**
     * Like {@link #respondToUser(ChatChannel, ChatRequest)} but with a caller-supplied
     * prompt message in place of the raw request text (e.g. a wx command that spells
     * out the weather request for the model).
     */
    public Optional<String> respondToUser(ChatChannel chatChannel, ChatRequest request, String promptMessage) {
        return respond(chatChannel, request, promptMessage);
    }

    private Optional<String> respond(ChatChannel chatChannel, ChatRequest request, String promptMessage) {
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
        log.debug("respond: loaded {} messages, {} memories for channel {}", messages.size(), memories.size(), chatChannel.getChannelName());

        List<PromptMessage> promptMessages = new ArrayList<>(messages.stream().map(PromptMessage::from).toList());
        promptMessages.add(PromptMessage.now(request.senderName(), promptMessage));

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
            log.debug("respond: AI returned empty response");
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
