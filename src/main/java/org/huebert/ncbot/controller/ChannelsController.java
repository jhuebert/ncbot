package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.service.ChannelService;
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

    private final ChannelService channelService;
    private final ChatChannelRepository channelRepository;

    @DebugLog
    @GetMapping("/channels")
    public PageResponse<ChatChannel> channels(
            @RequestParam(required = false) Boolean dm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "channelName"));
        Page<ChatChannel> channels = dm != null
                ? channelRepository.findChannelsByDm(dm, pageable)
                : channelRepository.findAll(pageable);
        return PageResponse.fromPage(channels);
    }

    @DebugLog
    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long channelId) {
        channelService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

}
