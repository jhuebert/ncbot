package org.huebert.ncbot.service;

import com.google.common.base.Utf8;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.ChatResponse;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final ChatParticipantRepository chatParticipantRepository;

    public ChatService(ChatModel chatModel,
                       ChatMessageRepository chatMessageRepository,
                       NcbotProperties properties,
                       WeatherTool weatherTool,
                       TemplateService templateService,
                       ChatChannelRepository chatChannelRepository,
                       ChatMemory2Repository chatMemoryRepository,
                       ChatParticipantRepository chatParticipantRepository
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.properties = properties;
        this.templateService = templateService;
        this.chatChannelRepository = chatChannelRepository;
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(weatherTool)
                .build();
    }

    public ChatResponse processMessage(ChatRequest request) {
        log.debug("processMessage: {}", request);

        if (Boolean.TRUE.equals(request.isOutgoing())) {
            log.debug("skipping outgoing message");
            return EMPTY_RESPONSE;
        }

        try {
            ChatChannel chatChannel = getChatChannel(request);
            Optional<String> response = generateResponse(chatChannel, request);
            saveInteraction(chatChannel, request, response.orElse(null));
            return response
                    .map(r -> truncateUtf8(r, properties.maxReplyBytes()))
                    .map(r -> new ChatResponse(List.of(r)))
                    .orElse(EMPTY_RESPONSE);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            return EMPTY_RESPONSE;
        }
    }

    private ChatChannel getChatChannel(ChatRequest request) {
        return chatChannelRepository.findChannel(request.isDm(), request.isDm() ? request.senderKey() : request.channelKey())
                .orElseGet(() -> chatChannelRepository.saveAndFlush(ChatChannel.builder()
                        .channelKey(request.isDm() ? request.senderKey() : request.channelKey())
                        .channelName(request.isDm() ? request.senderName() : request.channelName())
                        .isDm(request.isDm())
                        .memoryUpdatedAt(Instant.EPOCH)
                        .build()));
    }

    private void saveInteraction(ChatChannel chatChannel, ChatRequest request, String response) {
        log.debug("saveInteraction: request={}, response={}", request.messageText(), response);
        chatMessageRepository.save(ChatMessage.builder()
                .chatChannelId(chatChannel.getId())
                .content(request.messageText())
                .createdAt(Instant.now())
                .senderName(request.senderName())
                .build());
        if (response != null) {
            chatMessageRepository.save(ChatMessage.builder()
                    .chatChannelId(chatChannel.getId())
                    .content(response)
                    .createdAt(Instant.now())
                    .senderName("ncbot")
                    .build());
        }
    }

    private Optional<String> generateResponse(ChatChannel chatChannel, ChatRequest request) {
        log.debug("generateResponse: chatChannel={}, request={}", chatChannel, request);

        NcbotProperties.ChannelProperties channelProperties;
        if (request.isDm()) {
            Set<String> allowed = properties.allowedDms();
            if ((allowed != null) && !allowed.isEmpty() && ((request.senderKey() == null) || !allowed.contains(request.senderKey()))) {
                log.debug("skipping DM {}", request.senderKey());
                return Optional.empty();
            }
            channelProperties = new NcbotProperties.ChannelProperties(request.senderName(), true, false, true);
        } else {
            channelProperties = properties.channels().stream()
                    .filter(a -> a.name().equals(request.channelName()))
                    .findFirst()
                    .orElse(null);
            if (channelProperties == null) {
                log.debug("skipping channel {}", request.channelName());
                return Optional.empty();
            }
        }

        ChatParticipant participant = chatParticipantRepository.findParticipant(request.senderName()).orElse(null);
        if (participant != null) {
            participant.setLastSeen(Instant.now());
            chatParticipantRepository.save(participant);
        } else {
            log.debug("new participant {}", request.senderName());
            chatParticipantRepository.save(ChatParticipant.builder()
                    .name(request.senderName())
                    .lastSeen(Instant.now())
                    .build());
            if (channelProperties.welcome()) {
                log.debug("sending welcome to {}", request.senderName());
                return Optional.of(templateService.render("welcome", Map.of(
                        "request", request,
                        "welcomeContent", properties.welcomeContent()
                )));
            }
        }

        String message = Optional.ofNullable(request.messageText())
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse("");
        if (channelProperties.command() && properties.commands().contains(message)) {
            log.debug("command {}", message);
            return Optional.of(templateService.render("standard", Map.of(
                    "request", request
            )));
        }

        if (channelProperties.ai()) {
            return invokeAi(chatChannel, request);
        }

        return Optional.empty();
    }

    private Optional<String> invokeAi(ChatChannel chatChannel, ChatRequest request) {
        log.debug("invokeAi: {}", request);

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

    public static String truncateUtf8(String text, int maxBytes) {

        if (Utf8.encodedLength(text) <= maxBytes) {
            return text;
        }

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int limit = maxBytes - 3;
        while ((bytes[limit] & 0xC0) == 0x80) {
            limit--;
        }

        return new String(bytes, 0, limit, StandardCharsets.UTF_8) + "...";
    }

}
