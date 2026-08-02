package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.controller.dto.ParticipantDto;
import org.huebert.ncbot.controller.dto.ParticipantUpdateRequest;
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
    public PageResponse<ParticipantDto> participants(
            @PathVariable Long channelId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.fromPage(participantService.findParticipantsByChannel(channelId, query, pageable), ParticipantDto::from);
    }

    @DebugLog
    @GetMapping("/participants")
    public PageResponse<ParticipantDto> allParticipants(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.fromPage(participantService.findAllParticipants(query, pageable), ParticipantDto::from);
    }

    @DebugLog
    @PutMapping("/participants/{participantId}")
    public ParticipantDto updateParticipant(
            @PathVariable Long participantId,
            @RequestBody ParticipantUpdateRequest request
    ) {
        return ParticipantDto.from(participantService.updateBlocked(participantId, request.blocked()));
    }

}
