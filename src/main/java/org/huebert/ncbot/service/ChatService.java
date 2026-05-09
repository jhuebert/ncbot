package org.huebert.ncbot.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.ChatResponse;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.tool.MessageHistoryTool;
import org.huebert.ncbot.util.MessageSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ChatMessageRepository messageRepository;
    private final NcbotProperties properties;
    private final MessageHistoryTool messageHistoryTool;

    public ChatService(ChatModel chatModel,
                       ChatMessageRepository messageRepository,
                       NcbotProperties properties,
                       MessageHistoryTool messageHistoryTool,
                       List<ToolCallbackProvider> toolCallbackProviders) {
        this.messageRepository = messageRepository;
        this.properties = properties;
        this.messageHistoryTool = messageHistoryTool;

        this.chatClient = ChatClient.builder(chatModel)
            .defaultSystem(properties.systemPrompt())
            .defaultToolCallbacks(toolCallbackProviders.toArray(new ToolCallbackProvider[0]))
            .build();
    }

    public ChatResponse processMessage(ChatRequest request) {
        // Phase 1: Persist the message
        ChatMessage entity = new ChatMessage(
            request.senderName(), request.senderKey(), request.messageText(),
            request.isDm(), request.channelKey(), request.channelName(),
            request.senderTimestamp(), request.path(), request.isOutgoing(),
            request.pathBytesPerHop()
        );
        messageRepository.save(entity);

        // Phase 2: Filter — skip outgoing messages
        if (Boolean.TRUE.equals(request.isOutgoing())) {
            log.debug("Skipping outgoing message from {}", request.senderName());
            return new ChatResponse(List.of());
        }

        // Phase 3: DM check
        if (Boolean.TRUE.equals(request.isDm())) {
            if (!properties.allowDms()) {
                log.debug("Skipping DM — DMs not allowed");
                return new ChatResponse(List.of());
            }
        }

        // Phase 4: Channel check
        if (Boolean.FALSE.equals(request.isDm())) {
            List<String> allowed = properties.allowedChannels();
            if (allowed != null && !allowed.isEmpty()) {
                if (request.channelKey() == null || !allowed.contains(request.channelKey())) {
                    log.debug("Skipping channel {} — not in allowed list", request.channelKey());
                    return new ChatResponse(List.of());
                }
            }
        }

        // Phase 5: Apply configurable delay
        double delay = properties.responseDelaySeconds();
        if (delay > 0) {
            try {
                TimeUnit.SECONDS.sleep((long) delay);
                if (delay % 1 > 0) {
                    Thread.sleep((long) ((delay % 1) * 1000));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted during response delay");
                return new ChatResponse(List.of());
            }
        }

        // Phase 6: Build context and call AI
        try {
            return callAi(request);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            return new ChatResponse(List.of());
        }
    }

    private ChatResponse callAi(ChatRequest request) {
        // Set context for message history tool
        messageHistoryTool.setContext(request.channelKey(), request.isDm(), request.senderKey());

        try {
            // Build system prompt (use override if provided)
            String systemPrompt = request.systemPrompt() != null
                ? request.systemPrompt()
                : properties.systemPrompt();

            // Build user message with message details for context
            String userMessage = buildUserMessage(request);

            // Call AI with tools
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

            if (response == null || response.isBlank()) {
                return new ChatResponse(List.of());
            }

            // Enforce byte limit and split
            List<String> replies = ensureByteLimit(response);
            return new ChatResponse(replies);
        } finally {
            messageHistoryTool.clearContext();
        }
    }

    private String buildUserMessage(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.messageText());

        // Add context about the message for the AI
        sb.append("\n\n<Message details>");
        if (request.senderName() != null) {
            sb.append("\nSender: ").append(request.senderName());
        }
        if (request.channelName() != null) {
            sb.append("\nChannel: ").append(request.channelName());
        }
        if (request.path() != null) {
            sb.append("\nPath: ").append(request.path());
        }
        if (request.pathBytesPerHop() != null) {
            sb.append("\nPath bytes per hop: ").append(request.pathBytesPerHop());
        }
        if (request.isDm() != null) {
            sb.append("\nType: ").append(Boolean.TRUE.equals(request.isDm()) ? "DM" : "Channel");
        }
        sb.append("\n</Message details>");

        return sb.toString();
    }

    private List<String> ensureByteLimit(String text) {
        int maxBytes = properties.maxReplyBytes();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        if (bytes.length <= maxBytes) {
            return List.of(text);
        }

        // Try splitting at sentence boundaries
        return MessageSplitter.splitIntoMessages(text, maxBytes);
    }
}
