package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ParticpantsController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;

    @GetMapping("/channels/{channelId}/participants")
    public PageResponse<ChatParticipant> participants(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        log.info("admin: fetching participants (channel={}, page={}, size={})", channelId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Set<String> senders = messageRepository.findSenderNamesByChannel(channelId);
        Page<ChatParticipant> participants = participantRepository.findParticipants(senders, pageable);
        log.debug("admin: returning {} senders for channel {}", participants.getTotalElements(), channelId);
        return PageResponse.fromPage(participants);
    }

    @GetMapping("/participants")
    public PageResponse<ChatParticipant> allParticipants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        log.info("admin: fetching all participants (page={}, size={})", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<ChatParticipant> all = participantRepository.findLastSeen(pageable);
        log.debug("admin: returning {} participants", all.getTotalElements());
        return PageResponse.fromPage(all);
    }

}
