package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.ChannelDto;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.service.ChannelService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ChannelsController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ChannelService channelService;

    @DebugLog
    @GetMapping("/channels")
    public PageResponse<ChannelDto> channels(
            @RequestParam(required = false) Boolean dm,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.fromPage(channelService.findChannels(dm, query, pageable));
    }

    @DebugLog
    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long channelId) {
        channelService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

}
