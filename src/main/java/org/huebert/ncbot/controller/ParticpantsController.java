package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ParticpantsController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;

    @DebugLog
    @GetMapping("/channels/{channelId}/participants")
    public PageResponse<ChatParticipant> participants(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.fromPage(participantRepository.findParticipants(messageRepository.findSenderNamesByChannel(channelId), pageable));
    }

    @DebugLog
    @GetMapping("/participants")
    public PageResponse<ChatParticipant> allParticipants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.fromPage(participantRepository.findLastSeen(pageable));
    }

}
