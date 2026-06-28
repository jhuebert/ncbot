package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.MemoryDto;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.repository.ChatMemory2Repository;
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

    private final ChatMemory2Repository memoryRepository;

    @DebugLog
    @GetMapping("/channels/{channelId}/memory")
    public PageResponse<MemoryDto> getChannelMemory(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "key"));
        return PageResponse.fromPage(memoryRepository.findChannelMemory(channelId, pageable), MemoryDto::from);
    }

    @DebugLog
    @GetMapping("/memory")
    public PageResponse<MemoryDto> getGlobalMemory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "key"));
        return PageResponse.fromPage(memoryRepository.findGlobalMemory(pageable), MemoryDto::from);
    }

    @DebugLog
    @PostMapping("/channels/{channelId}/memory")
    public MemoryDto createChannelMemory(
            @PathVariable Long channelId,
            @RequestBody MemoryDto request
    ) {
        ChatMemory memory = ChatMemory.builder()
                .chatChannelId(channelId)
                .key(request.key())
                .value(request.value())
                .build();
        return MemoryDto.from(memoryRepository.save(memory));
    }

    @DebugLog
    @PostMapping("/memory")
    public MemoryDto createGlobalMemory(
            @RequestBody MemoryDto request
    ) {
        ChatMemory memory = ChatMemory.builder()
                .chatChannelId(null)
                .key(request.key())
                .value(request.value())
                .build();
        return MemoryDto.from(memoryRepository.save(memory));
    }

    @DebugLog
    @PutMapping("/channels/{channelId}/memory/{id}")
    public MemoryDto updateChannelMemory(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestBody MemoryDto request
    ) {
        ChatMemory memory = requireChannelMemory(channelId, id);
        memory.setKey(request.key());
        memory.setValue(request.value());
        return MemoryDto.from(memoryRepository.save(memory));
    }

    @DebugLog
    @PutMapping("/memory/{id}")
    public MemoryDto updateGlobalMemory(
            @PathVariable Long id,
            @RequestBody MemoryDto request
    ) {
        ChatMemory memory = requireGlobalMemory(id);
        memory.setKey(request.key());
        memory.setValue(request.value());
        return MemoryDto.from(memoryRepository.save(memory));
    }

    @DebugLog
    @DeleteMapping("/channels/{channelId}/memory/{id}")
    public ResponseEntity<Void> deleteChannelMemory(
            @PathVariable Long channelId,
            @PathVariable Long id
    ) {
        memoryRepository.delete(requireChannelMemory(channelId, id));
        return ResponseEntity.noContent().build();
    }

    @DebugLog
    @DeleteMapping("/memory/{id}")
    public ResponseEntity<Void> deleteGlobalMemory(
            @PathVariable Long id
    ) {
        memoryRepository.delete(requireGlobalMemory(id));
        return ResponseEntity.noContent().build();
    }

    @DebugLog
    @PostMapping("/channels/{channelId}/memory/{id}/promote")
    public MemoryDto promoteMemory(
            @PathVariable Long channelId,
            @PathVariable Long id
    ) {
        ChatMemory source = requireChannelMemory(channelId, id);
        ChatMemory promoted = ChatMemory.builder()
                .chatChannelId(null)
                .key(source.getKey())
                .value(source.getValue())
                .build();
        memoryRepository.save(promoted);
        memoryRepository.delete(source);
        return MemoryDto.from(promoted);
    }

    private ChatMemory requireChannelMemory(Long channelId, Long id) {
        ChatMemory memory = memoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + id));
        if (!channelId.equals(memory.getChatChannelId())) {
            throw new IllegalArgumentException("Memory does not belong to channel " + channelId);
        }
        return memory;
    }

    private ChatMemory requireGlobalMemory(Long id) {
        ChatMemory memory = memoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + id));
        if (memory.getChatChannelId() != null) {
            throw new IllegalArgumentException("Memory is not global");
        }
        return memory;
    }

}
