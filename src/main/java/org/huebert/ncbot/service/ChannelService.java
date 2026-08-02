package org.huebert.ncbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.ChannelDto;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.util.DebugLog;
import org.huebert.ncbot.util.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChatChannelRepository channelRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMemory2Repository memoryRepository;

    @DebugLog
    @Transactional
    public void deleteChannel(Long channelId) {
        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        messageRepository.deleteByChatChannelId(channelId);
        memoryRepository.deleteByChatChannelId(channelId);
        channelRepository.delete(channel);
        log.info("Deleted channel {} ({})", channel.getChannelName(), channelId);
    }

    @DebugLog
    @Transactional
    public ChatChannel getChatChannel(ChatRequest request) {
        return channelRepository.findChannel(request.isDm(), request.isDm() ? request.senderKey() : request.channelKey())
                .orElseGet(() -> {
                    ChatChannel newChannel = ChatChannel.builder()
                            .channelKey(request.isDm() ? request.senderKey() : request.channelKey())
                            .channelName(request.isDm() ? request.senderName() : request.channelName())
                            .isDm(request.isDm())
                            .memoryUpdatedAt(Instant.EPOCH)
                            .build();
                    log.debug("creating new channel: name={}, dm={}", newChannel.getChannelName(), newChannel.getIsDm());
                    return channelRepository.saveAndFlush(newChannel);
                });
    }

    @DebugLog
    @Transactional(readOnly = true)
    public List<ChatChannel> findAll() {
        return channelRepository.findAll();
    }

    /**
     * Lists channels with their most recent message timestamp, sorted by last
     * message descending (channels without messages sort last), then by name.
     * An optional {@code query} filters by case-insensitive substring match on
     * channel name or key. Pagination is applied after sorting since last
     * message time is computed.
     */
    @DebugLog
    @Transactional(readOnly = true)
    public Page<ChannelDto> findChannels(Boolean dm, String query, Pageable pageable) {
        String q = normalized(query);
        List<ChatChannel> channels = dm == null
                ? channelRepository.findAll()
                : channelRepository.findChannelsByDm(dm);

        Map<Long, Instant> lastMessageAt = messageRepository.findLastMessageByChannel().stream()
                .collect(Collectors.toMap(Pair::key, Pair::value));

        List<ChannelDto> sorted = channels.stream()
                .filter(c -> q == null || matches(q, c.getChannelName()) || matches(q, c.getChannelKey()))
                .map(c -> ChannelDto.from(c, lastMessageAt.get(c.getId())))
                .sorted(Comparator
                        .comparing(ChannelDto::lastMessageAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ChannelDto::channelName, Comparator.nullsLast(String::compareTo)))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        List<ChannelDto> slice = start >= sorted.size() ? List.of() : sorted.subList(start, end);
        return new PageImpl<>(slice, pageable, sorted.size());
    }

    @DebugLog
    @Transactional
    public void setMemoryUpdated(Long id) {
        channelRepository.setMemoryUpdated(id, Instant.now());
    }

    private static String normalized(String query) {
        return query == null ? null : query.trim();
    }

    private static boolean matches(String query, String value) {
        return value != null && value.toLowerCase().contains(query.toLowerCase());
    }

}
