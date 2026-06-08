package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMessageRepository;
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

    @GetMapping("/channels/{channelId}/messages")
    public PageResponse<ChatMessage> messages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Instant before,
            @RequestParam(required = false) Instant after,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        log.info("admin: fetching messages for channel {} (page={}, size={}, before={}, after={}, sort={})",
                channelId, page, size, before, after, sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "createdAt"));
        Page<ChatMessage> messages;
        if (before != null && after != null) {
            messages = messageRepository.findMessagesByChannelBetween(channelId, before, after, pageable);
        } else if (before != null) {
            messages = messageRepository.findMessagesByChannelBefore(channelId, before, pageable);
        } else if (after != null) {
            messages = messageRepository.findMessagesByChannelAfter(channelId, after, pageable);
        } else {
            messages = messageRepository.findMessagesByChannelOrderByCreatedDesc(channelId, pageable);
        }
        log.debug("admin: returning {} messages for channel {}", messages.getTotalElements(), channelId);
        return PageResponse.fromPage(messages);
    }

}
