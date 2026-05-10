package org.huebert.ncbot.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.ChatResponse;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.tool.CountBytesTool;
import org.huebert.ncbot.tool.CurrentTimeTool;
import org.huebert.ncbot.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatService {

    private static final ChatResponse EMPTY_RESPONSE = new ChatResponse(List.of());

    private static final String SYSTEM_PROMPT = """
            %s
            Message History:
            
            %s
            """;

    private static final String CONDENSE_PROMPT = """
            Your task is to condense the user message so that it is no more than %d UTF-8 bytes.
            It is currently %d bytes long.
            The user message is in response to: %s
            """;

    private static final String MESSAGE_FORMAT = "%s: %s";

    private final ChatClient chatClient;
    private final ChatMessageRepository messageRepository;
    private final NcbotProperties properties;

    public ChatService(ChatModel chatModel,
                       ChatMessageRepository messageRepository,
                       NcbotProperties properties,
                       CountBytesTool countBytesTool,
                       WeatherTool weatherTool,
                       CurrentTimeTool currentTimeTool
    ) {
        this.messageRepository = messageRepository;
        this.properties = properties;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(currentTimeTool, countBytesTool, weatherTool)
                .build();
    }

    public ChatResponse processMessage(ChatRequest request) {
        log.info("processMessage: {}", request);

        if (Boolean.TRUE.equals(request.isOutgoing())) {
            log.info("Skipping outgoing message from {}", request.senderName());
            return EMPTY_RESPONSE;
        }

        if (Boolean.TRUE.equals(request.isDm())) {
            if (!properties.allowDms()) {
                log.info("Skipping DM — DMs not allowed");
                return EMPTY_RESPONSE;
            }
        }

        if (Boolean.FALSE.equals(request.isDm())) {
            List<String> allowed = properties.allowedChannels();
            if (allowed != null && !allowed.isEmpty()) {
                if (request.channelName() == null || !allowed.contains(request.channelName())) {
                    log.info("Skipping channel {} — not in allowed list", request.channelName());
                    return EMPTY_RESPONSE;
                }
            }
        }

        try {
            String response = invokeModel(request);
            saveInteraction(request, response);
            log.info("processMessage result: {}", response);
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
        String userMessage = buildUserMessage(request);
        log.info("userMessage: {}", userMessage);

        Instant since = ZonedDateTime.now().minusMinutes(properties.messageHistoryMinutes()).toInstant();

        List<ChatMessage> ms;
        if (Boolean.TRUE.equals(request.isDm())) {
            ms = messageRepository.findLatestDms(request.senderKey(), since);
        } else {
            ms = messageRepository.findLatestChannelMessages(request.channelKey(), since);
        }
        String messages = ms.stream()
                .map(this::buildUserMessage)
                .collect(Collectors.joining("\n"));

        String systemPrompt = String.format(SYSTEM_PROMPT, properties.systemPrompt(), messages);
        log.info("systemPrompt: {}", systemPrompt);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

        return ensureByteLimit(userMessage, response);
    }

    private String buildUserMessage(ChatRequest request) {
        return String.format(MESSAGE_FORMAT, request.senderName(), request.messageText());
    }

    private String buildUserMessage(ChatMessage message) {
        return String.format(MESSAGE_FORMAT, message.getSenderName(), message.getMessageText());
    }

    private String ensureByteLimit(String user, String system) {
        log.info("ensureByteLimit: {}", system);

        int systemLength = system.getBytes(StandardCharsets.UTF_8).length;
        if (systemLength <= properties.maxReplyBytes()) {
            log.info("ensureByteLimit result: {}", system);
            return system;
        }

        String response = chatClient.prompt()
                .system(String.format(CONDENSE_PROMPT, properties.maxReplyBytes(), systemLength, user))
                .user(system)
                .call()
                .content();

        log.info("ensureByteLimit result: {}", response);
        return response;
    }

}
