package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.MessagesResponse;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class MessagesController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ChatMessageRepository messageRepository;
    private final ChatChannelRepository channelRepository;

    @DebugLog
    @GetMapping("/channels/{channelId}/messages")
    public MessagesResponse messages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Instant before,
            @RequestParam(required = false) Instant after,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "createdAt"));
        Page<ChatMessage> result;
        if (before != null && after != null) {
            result = messageRepository.findMessagesByChannelBetween(channelId, before, after, pageable);
        } else if (before != null) {
            result = messageRepository.findMessagesByChannelBefore(channelId, before, pageable);
        } else if (after != null) {
            result = messageRepository.findMessagesByChannelAfter(channelId, after, pageable);
        } else {
            result = messageRepository.findMessagesByChannelOrderByCreatedDesc(channelId, pageable);
        }
        String channelName = channelRepository.findById(channelId).map(ChatChannel::getChannelName).orElse(null);
        return MessagesResponse.fromPage(channelId, channelName, result);
    }

}
