package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.MemoryDto;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.service.MemoryService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class MemoryController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final MemoryService memoryService;

    @DebugLog
    @GetMapping("/channels/{channelId}/memory")
    public PageResponse<MemoryDto> getChannelMemory(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "key"));
        return PageResponse.fromPage(memoryService.findChannelMemory(channelId, pageable), MemoryDto::from);
    }

    @DebugLog
    @GetMapping("/memory")
    public PageResponse<MemoryDto> getGlobalMemory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "key"));
        return PageResponse.fromPage(memoryService.findGlobalMemory(pageable), MemoryDto::from);
    }

    @DebugLog
    @PostMapping("/channels/{channelId}/memory")
    public MemoryDto createChannelMemory(
            @PathVariable Long channelId,
            @RequestBody MemoryDto request
    ) {
        return MemoryDto.from(memoryService.createChannelMemory(channelId, request.key(), request.value()));
    }

    @DebugLog
    @PostMapping("/memory")
    public MemoryDto createGlobalMemory(
            @RequestBody MemoryDto request
    ) {
        return MemoryDto.from(memoryService.createGlobalMemory(request.key(), request.value()));
    }

    @DebugLog
    @PutMapping("/channels/{channelId}/memory/{id}")
    public MemoryDto updateChannelMemory(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestBody MemoryDto request
    ) {
        return MemoryDto.from(memoryService.updateChannelMemory(channelId, id, request.key(), request.value()));
    }

    @DebugLog
    @PutMapping("/memory/{id}")
    public MemoryDto updateGlobalMemory(
            @PathVariable Long id,
            @RequestBody MemoryDto request
    ) {
        return MemoryDto.from(memoryService.updateGlobalMemory(id, request.key(), request.value()));
    }

    @DebugLog
    @DeleteMapping("/channels/{channelId}/memory/{id}")
    public ResponseEntity<Void> deleteChannelMemory(
            @PathVariable Long channelId,
            @PathVariable Long id
    ) {
        memoryService.deleteChannelMemory(channelId, id);
        return ResponseEntity.noContent().build();
    }

    @DebugLog
    @DeleteMapping("/memory/{id}")
    public ResponseEntity<Void> deleteGlobalMemory(
            @PathVariable Long id
    ) {
        memoryService.deleteGlobalMemory(id);
        return ResponseEntity.noContent().build();
    }

    @DebugLog
    @PostMapping("/channels/{channelId}/memory/{id}/promote")
    public MemoryDto promoteMemory(
            @PathVariable Long channelId,
            @PathVariable Long id
    ) {
        return MemoryDto.from(memoryService.promoteMemory(channelId, id));
    }

}
