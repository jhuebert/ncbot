package org.huebert.ncbot.service;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.ChatResponse;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.entity.ConversationMemory;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ConversationMemoryRepository;
import org.huebert.ncbot.tool.CountBytesTool;
import org.huebert.ncbot.tool.WeatherTool;
import org.huebert.ncbot.util.MessageFormatter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private static final ChatResponse EMPTY_RESPONSE = new ChatResponse(List.of());


    private static final String USER_PROMPT = """
            ## Conversation Memory
            
            %s
            
            ## Additional Context
            
            Current Time: %s
            
            ## Latest Messages
            
            %s
            """;

    private static final String CONDENSE_PROMPT = """
            Request: %s
            Response: %s
            Current Length: %s
            """;

    private final ChatClient chatClient;
    private final ChatMessageRepository messageRepository;
    private final ConversationMemoryRepository conversationMemoryRepository;
    private final NcbotProperties properties;

    public ChatService(ChatModel chatModel,
                       ChatMessageRepository messageRepository,
                       NcbotProperties properties,
                       CountBytesTool countBytesTool,
                       WeatherTool weatherTool,
                       ConversationMemoryRepository conversationMemoryRepository
    ) {
        this.messageRepository = messageRepository;
        this.properties = properties;
        this.conversationMemoryRepository = conversationMemoryRepository;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(countBytesTool, weatherTool)
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
            String response = invokeModel(request);
            saveInteraction(request, response);
            log.debug("processMessage result: {}", response);
            return new ChatResponse(List.of(response));
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            return EMPTY_RESPONSE;
        }
    }

    private void saveInteraction(ChatRequest request, String response) {
        ChatMessage user = ChatMessage.builder()
                .channelName(request.channelName())
                .createdAt(Instant.now())
                .path(request.path())
                .channelKey(request.channelKey())
                .isDm(request.isDm())
                .messageText(request.messageText())
                .isOutgoing(request.isOutgoing())
                .pathBytesPerHop(request.pathBytesPerHop())
                .senderTimestamp(request.senderTimestamp())
                .senderKey(request.senderKey())
                .senderName(request.senderName())
                .build();
        ChatMessage system = user.toBuilder()
                .senderName("ncbot")
                .messageText(response)
                .isOutgoing(true)
                .build();
        messageRepository.saveAll(List.of(user, system));
    }

    private String invokeModel(ChatRequest request) {
        String userMessage = MessageFormatter.buildUserMessage(request);
        log.debug("userMessage: {}", userMessage);

        List<ChatMessage> ms;
        Optional<ConversationMemory> memory;
        if (Boolean.TRUE.equals(request.isDm())) {
            memory = conversationMemoryRepository.findDmMemory(request.senderKey());
            Instant since = memory
                    .map(ConversationMemory::getUpdatedAt)
                    .orElse(ZonedDateTime.now().minusYears(10).toInstant());
            ms = messageRepository.findLatestDms(request.senderKey(), since);
        } else {
            memory = conversationMemoryRepository.findChannelMemory(request.channelKey());
            Instant since = memory
                    .map(ConversationMemory::getUpdatedAt)
                    .orElse(ZonedDateTime.now().minusYears(10).toInstant());
            ms = messageRepository.findLatestChannelMessages(request.channelKey(), since);
        }

        String messages = ms.stream()
                .map(MessageFormatter::buildUserMessage)
                .collect(Collectors.joining("\n"));
        messages += "\n" + userMessage;

        String memoryContent = memory.map(ConversationMemory::getContent).orElse("no previous memory");
        String user = String.format(USER_PROMPT, memoryContent, ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), messages);
        log.info("user = {}", user);
        String response = chatClient.prompt()
                .system(properties.systemPrompt())
                .user(user)
                .messages()
                .call()
                .content();

        return ensureByteLimit(userMessage, response);
    }

    private String ensureByteLimit(String user, String system) {
        log.debug("ensureByteLimit: {}", system);

        int systemLength = system.getBytes(StandardCharsets.UTF_8).length;
        if (systemLength <= properties.maxReplyBytes()) {
            log.debug("ensureByteLimit result: {}", system);
            return system;
        }

        String response = chatClient.prompt()
                .system(properties.condensePrompt())
                .user(String.format(CONDENSE_PROMPT, user, system, systemLength))
                .call()
                .content();

        log.debug("ensureByteLimit result: {}", response);
        return response;
    }

}
