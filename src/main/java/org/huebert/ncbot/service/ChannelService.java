package org.huebert.ncbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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

    @DebugLog
    @Transactional
    public void setMemoryUpdated(Long id) {
        channelRepository.setMemoryUpdated(id, Instant.now());
    }

}
