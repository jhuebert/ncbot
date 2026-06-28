package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.service.ParticipantService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ParticipantsController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ParticipantService participantService;

    @DebugLog
    @GetMapping("/channels/{channelId}/participants")
    public PageResponse<ChatParticipant> participants(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.fromPage(participantService.findParticipantsByChannel(channelId, pageable));
    }

    @DebugLog
    @GetMapping("/participants")
    public PageResponse<ChatParticipant> allParticipants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.fromPage(participantService.findAllParticipants(pageable));
    }

}
