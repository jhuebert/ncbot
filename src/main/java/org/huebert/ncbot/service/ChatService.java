package org.huebert.ncbot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.ChatResponse;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChatService {

    private static final ChatResponse EMPTY_RESPONSE = new ChatResponse(List.of());

    private final ChatClient chatClient;
    private final NcbotProperties properties;
    private final TemplateService templateService;
    private final ChatChannelRepository chatChannelRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemory2Repository chatMemoryRepository;

    public ChatService(ChatModel chatModel,
                       ChatMessageRepository chatMessageRepository,
                       NcbotProperties properties,
                       WeatherTool weatherTool,
                       TemplateService templateService,
                       ChatChannelRepository chatChannelRepository,
                       ChatMemory2Repository chatMemoryRepository
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.properties = properties;
        this.templateService = templateService;
        this.chatChannelRepository = chatChannelRepository;
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(weatherTool)
                .build();
    }

    public ChatResponse processMessage(ChatRequest request) {
        log.debug("processMessage: {}", request);

        if (Boolean.TRUE.equals(request.isOutgoing())) {
            log.debug("Skipping outgoing message from {}", request.senderName());
            return EMPTY_RESPONSE;
        }

        if (Boolean.TRUE.equals(request.isDm())) {
            if (!properties.allowDms()) {
                log.debug("Skipping DM — DMs not allowed");
                return EMPTY_RESPONSE;
            }
        }

        if (Boolean.FALSE.equals(request.isDm())) {
            List<String> allowed = properties.allowedChannels();
            if (allowed != null && !allowed.isEmpty()) {
                if (request.channelName() == null || !allowed.contains(request.channelName())) {
                    log.debug("Skipping channel {} — not in allowed list", request.channelName());
                    return EMPTY_RESPONSE;
                }
            }
        }

        try {
            ChatChannel chatChannel = chatChannelRepository.findChannel(request.isDm(), request.isDm() ? request.senderKey() : request.channelKey())
                    .orElseGet(() -> chatChannelRepository.saveAndFlush(ChatChannel.builder()
                            .channelKey(request.isDm() ? request.senderKey() : request.channelKey())
                            .channelName(request.isDm() ? request.senderName() : request.channelName())
                            .isDm(request.isDm())
                            .memoryUpdatedAt(Instant.EPOCH)
                            .build()));
            String response = invokeModel(chatChannel, request);
            saveInteraction(chatChannel, request, response);
            log.debug("processMessage result: {}", response);
            return new ChatResponse(List.of(response));
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            return EMPTY_RESPONSE;
        }
    }

    private void saveInteraction(ChatChannel chatChannel, ChatRequest request, String response) {
        chatMessageRepository.save(ChatMessage.builder()
                .chatChannelId(chatChannel.getId())
                .content(request.messageText())
                .createdAt(Instant.now())
                .senderName(request.senderName())
                .build());
        if (Strings.trimToNull(response) != null) {
            chatMessageRepository.save(ChatMessage.builder()
                    .chatChannelId(chatChannel.getId())
                    .content(response)
                    .createdAt(Instant.now())
                    .senderName("ncbot")
                    .build());
        }
    }

    private String invokeModel(ChatChannel chatChannel, ChatRequest request) {
        log.debug("invokeModel: {}", request);

        List<ChatMessage> messages = chatMessageRepository.findChannelMessages(chatChannel.getId(), chatChannel.getMemoryUpdatedAt(), Instant.now());
        List<ChatMemory> memories = chatMemoryRepository.findMemory(chatChannel.getId());

        String output = templateService.render("chat", Map.of(
                "memories", memories,
                "messages", messages,
                "request", request
        ));

        String response = chatClient.prompt()
                .system(properties.systemPrompt())
                .user(output)
                .messages()
                .call()
                .content();

        if ("EMPTY".equalsIgnoreCase(Strings.trimToNull(response))) {
            return "";
        }

        return ensureByteLimit(request, response);
    }

    private String ensureByteLimit(ChatRequest request, String response) {
        log.debug("ensureByteLimit: {}", response);

        if (response.getBytes(StandardCharsets.UTF_8).length <= properties.maxReplyBytes()) {
            log.debug("ensureByteLimit result: {}", response);
            return response;
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

        log.debug("ensureByteLimit result: {}", condensed);
        return condensed;
    }

}
