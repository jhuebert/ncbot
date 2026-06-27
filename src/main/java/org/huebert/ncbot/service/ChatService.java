package org.huebert.ncbot.service;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.ChannelCapabilities;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.ChatResponse;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.handler.ChatHandler;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.util.Truncate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ChatService {

    private static final ChatResponse EMPTY_RESPONSE = new ChatResponse(List.of());

    private final NcbotProperties properties;
    private final ChatChannelRepository chatChannelRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final List<ChatHandler> handlers;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            NcbotProperties properties,
            ChatChannelRepository chatChannelRepository,
            List<ChatHandler> handlers
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.properties = properties;
        this.chatChannelRepository = chatChannelRepository;

        this.handlers = new ArrayList<>(handlers);
        Collections.sort(this.handlers);
    }

    public ChatResponse processMessage(ChatRequest request) {
        log.debug("processMessage: {}", request);

        if (Boolean.TRUE.equals(request.isOutgoing())) {
            log.debug("skipping outgoing message");
            return EMPTY_RESPONSE;
        }

        ChatChannel chatChannel = getChatChannel(request);
        Optional<String> response = generateResponse(chatChannel, request);
        saveInteraction(chatChannel, request, response.orElse(null));
        return response
                .map(r -> Truncate.truncateUtf8(r, properties.maxReplyBytes()))
                .map(r -> new ChatResponse(List.of(r)))
                .orElse(EMPTY_RESPONSE);
    }

    private ChatChannel getChatChannel(ChatRequest request) {
        return chatChannelRepository.findChannel(request.isDm(), request.isDm() ? request.senderKey() : request.channelKey())
                .orElseGet(() -> {
                    ChatChannel newChannel = ChatChannel.builder()
                            .channelKey(request.isDm() ? request.senderKey() : request.channelKey())
                            .channelName(request.isDm() ? request.senderName() : request.channelName())
                            .isDm(request.isDm())
                            .memoryUpdatedAt(Instant.EPOCH)
                            .build();
                    log.info("creating new channel: name={}, dm={}", newChannel.getChannelName(), newChannel.getIsDm());
                    return chatChannelRepository.saveAndFlush(newChannel);
                });
    }

    private void saveInteraction(ChatChannel chatChannel, ChatRequest request, String response) {
        log.debug("saveInteraction: request={}, response={}", request.messageText(), response);
        chatMessageRepository.save(ChatMessage.builder()
                .chatChannelId(chatChannel.getId())
                .content(request.messageText())
                .createdAt(Instant.now())
                .senderName(request.senderName())
                .response(response)
                .build());
    }

    private Optional<String> generateResponse(ChatChannel chatChannel, ChatRequest request) {
        log.debug("generateResponse: chatChannel={}, request={}", chatChannel, request);

        // DM authorization check
        if (request.isDm()) {
            if (!properties.allowedDms().isEmpty() && !properties.allowedDms().contains(request.senderKey())) {
                log.debug("skipping DM {}", request.senderKey());
                return Optional.empty();
            }
        }

        // Resolve channel capabilities — if empty, channel is not configured
        Optional<ChannelCapabilities> capsOpt = properties.getChannelCapabilities(request);
        if (capsOpt.isEmpty()) {
            log.debug("skipping channel {}", request.channelName());
            return Optional.empty();
        }

        for (ChatHandler handler : handlers) {
            String handlerName = handler.getClass().getSimpleName();
            log.debug("invoking handler: {}", handlerName);
            Optional<String> response = handler.handle(chatChannel, request);
            if (response.isPresent()) {
                if (response.get().equals(ChatHandler.DO_NOT_RESPOND)) {
                    log.debug("handler {} short-circuited the chain", handlerName);
                    return Optional.empty();
                }
                log.info("{} produced response for {} in {}", handlerName, request.senderName(), request.channelName());
                return response;
            }
        }

        log.debug("no handler produced a response");
        return Optional.empty();
    }


}
