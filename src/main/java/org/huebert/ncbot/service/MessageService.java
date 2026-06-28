package org.huebert.ncbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatMessageRepository chatMessageRepository;

    @DebugLog
    @Transactional
    public void saveInteraction(ChatChannel chatChannel, ChatRequest request, String response) {
        chatMessageRepository.save(ChatMessage.builder()
                .chatChannelId(chatChannel.getId())
                .content(request.messageText())
                .createdAt(Instant.now())
                .senderName(request.senderName())
                .response(response)
                .build());
    }

    @DebugLog
    @Transactional(readOnly = true)
    public List<ChatMessage> findChannelMessages(Long id, Instant start, Instant end) {
        return chatMessageRepository.findChannelMessages(id, start, end);
    }

}
