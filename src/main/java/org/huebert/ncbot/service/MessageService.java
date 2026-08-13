package org.huebert.ncbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.PromptMessage;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    /** Upper bound on how many messages an AI history query may return at once. */
    private static final int MAX_HISTORY_RESULTS = 500;

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
                .packetHash(request.packetHash())
                .region(request.region())
                .scoped(request.scoped())
                .build());
    }



    @DebugLog
    @Transactional(readOnly = true)
    public List<ChatMessage> findChannelMessages(Long id, Instant start, Instant end) {
        return chatMessageRepository.findChannelMessages(id, start, end);
    }

    /**
     * Query raw channel history on demand for an AI tool, newest-first in the DB for
     * an efficient bounded fetch, then re-ordered oldest-first so the tool result
     * matches the {@code __CHAT_MESSAGES__} prompt shape (see {@link PromptMessage}).
     *
     * @param channelId channel to search
     * @param start     inclusive lower bound; {@code null} for no bound
     * @param end       exclusive upper bound; {@code null} for no bound
     * @param sender    require exact sender name match; {@code null} for any
     * @param term      case-insensitive substring required in the message content; {@code null} for any
     * @param max       maximum number of messages to return (clamped to {@link #MAX_HISTORY_RESULTS})
     */
    @DebugLog
    @Transactional(readOnly = true)
    public List<PromptMessage> searchHistory(Long channelId, Instant start, Instant end,
                                             String sender, String term, int max) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(max, MAX_HISTORY_RESULTS)));
        List<ChatMessage> newest = chatMessageRepository.searchHistory(
                channelId, start, end, blankToNull(sender), blankToNull(term), pageable);
        List<PromptMessage> result = new ArrayList<>(newest.size());
        for (int i = newest.size() - 1; i >= 0; i--) {
            result.add(PromptMessage.from(newest.get(i)));
        }
        return result;
    }

    @DebugLog
    @Transactional(readOnly = true)
    public java.util.Optional<Instant> findCreatedAt(Long id) {
        return chatMessageRepository.findCreatedAt(id);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

}
