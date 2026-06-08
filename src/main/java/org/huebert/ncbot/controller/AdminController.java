package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.MemoryDto;
import org.huebert.ncbot.controller.dto.PageResponse;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminController {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ChatMessageRepository messageRepository;
    private final ChatChannelRepository channelRepository;
    private final ChatMemory2Repository memoryRepository;
    private final ChatParticipantRepository participantRepository;

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

    @GetMapping("/channels/{channelId}/memory")
    public PageResponse<MemoryDto> getChannelMemory(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        log.info("admin: fetching channel memory (channel={}, page={}, size={})", channelId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "key"));
        Page<ChatMemory> memories = memoryRepository.findChannelMemory(channelId, pageable);
        log.debug("admin: returning {} channel memories", memories.getTotalElements());
        return PageResponse.fromPage(memories, MemoryDto::from);
    }

    @GetMapping("/memory")
    public PageResponse<MemoryDto> getGlobalMemory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        log.info("admin: fetching global memory (page={}, size={})", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "key"));
        Page<ChatMemory> memories = memoryRepository.findGlobalMemory(pageable);
        log.debug("admin: returning {} global memories", memories.getTotalElements());
        return PageResponse.fromPage(memories, MemoryDto::from);
    }

    @PostMapping("/channels/{channelId}/memory")
    public MemoryDto createChannelMemory(
            @PathVariable Long channelId,
            @RequestBody MemoryDto request
    ) {
        log.info("admin: creating channel memory: channel={}, key={}", channelId, request.key());
        ChatMemory memory = ChatMemory.builder()
                .chatChannelId(channelId)
                .key(request.key())
                .value(request.value())
                .build();
        memoryRepository.save(memory);
        log.debug("admin: created channel memory id={}", memory.getId());
        return MemoryDto.from(memory);
    }

    @PostMapping("/memory")
    public MemoryDto createGlobalMemory(
            @RequestBody MemoryDto request
    ) {
        log.info("admin: creating global memory: key={}", request.key());
        ChatMemory memory = ChatMemory.builder()
                .chatChannelId(null)
                .key(request.key())
                .value(request.value())
                .build();
        memoryRepository.save(memory);
        log.debug("admin: created global memory id={}", memory.getId());
        return MemoryDto.from(memory);
    }

    @PutMapping("/channels/{channelId}/memory/{id}")
    public MemoryDto updateChannelMemory(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestBody MemoryDto request
    ) {
        log.info("admin: updating channel memory id={}", id);
        ChatMemory memory = requireChannelMemory(channelId, id);
        memory.setKey(request.key());
        memory.setValue(request.value());
        memoryRepository.save(memory);
        log.debug("admin: updated channel memory id={}", memory.getId());
        return MemoryDto.from(memory);
    }

    @PutMapping("/memory/{id}")
    public MemoryDto updateGlobalMemory(
            @PathVariable Long id,
            @RequestBody MemoryDto request
    ) {
        log.info("admin: updating global memory id={}", id);
        ChatMemory memory = requireGlobalMemory(id);
        memory.setKey(request.key());
        memory.setValue(request.value());
        memoryRepository.save(memory);
        log.debug("admin: updated global memory id={}", memory.getId());
        return MemoryDto.from(memory);
    }

    @DeleteMapping("/channels/{channelId}/memory/{id}")
    public ResponseEntity<Void> deleteChannelMemory(
            @PathVariable Long channelId,
            @PathVariable Long id
    ) {
        log.info("admin: deleting channel memory id={}", id);
        ChatMemory memory = requireChannelMemory(channelId, id);
        memoryRepository.delete(memory);
        log.debug("admin: deleted channel memory id={}", id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/memory/{id}")
    public ResponseEntity<Void> deleteGlobalMemory(
            @PathVariable Long id
    ) {
        log.info("admin: deleting global memory id={}", id);
        ChatMemory memory = requireGlobalMemory(id);
        memoryRepository.delete(memory);
        log.debug("admin: deleted global memory id={}", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/channels/{channelId}/memory/{id}/promote")
    public MemoryDto promoteMemory(
            @PathVariable Long channelId,
            @PathVariable Long id
    ) {
        log.info("admin: promoting memory id={} from channel {}", id, channelId);
        ChatMemory source = requireChannelMemory(channelId, id);

        ChatMemory promoted = ChatMemory.builder()
                .chatChannelId(null)
                .key(source.getKey())
                .value(source.getValue())
                .build();
        memoryRepository.save(promoted);
        memoryRepository.delete(source);
        log.debug("admin: promoted memory to global id={}", promoted.getId());
        return MemoryDto.from(promoted);
    }

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
