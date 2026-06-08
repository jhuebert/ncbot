package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ChannelsController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ChatChannelRepository channelRepository;

    @GetMapping("/channels")
    public PageResponse<ChatChannel> channels(
            @RequestParam(required = false) Boolean dm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        log.info("admin: fetching channels (dm={}, page={}, size={})", dm, page, size);
        Page<ChatChannel> channels;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "channelName"));
        if (dm != null) {
            channels = channelRepository.findChannelsByDm(dm, pageable);
        } else {
            channels = channelRepository.findAll(pageable);
        }
        log.debug("admin: found {} channels", channels.getTotalElements());
        return PageResponse.fromPage(channels);
    }

}
