package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ChannelsController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ChatChannelRepository channelRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMemory2Repository memoryRepository;

    @DebugLog
    @GetMapping("/channels")
    public PageResponse<ChatChannel> channels(
            @RequestParam(required = false) Boolean dm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Page<ChatChannel> channels;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "channelName"));
        if (dm != null) {
            channels = channelRepository.findChannelsByDm(dm, pageable);
        } else {
            channels = channelRepository.findAll(pageable);
        }
        return PageResponse.fromPage(channels);
    }

    @DebugLog
    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long channelId) {
        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        messageRepository.deleteByChatChannelId(channelId);
        memoryRepository.deleteByChatChannelId(channelId);
        channelRepository.delete(channel);
        return ResponseEntity.noContent().build();
    }

}
