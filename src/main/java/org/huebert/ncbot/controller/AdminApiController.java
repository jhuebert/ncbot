package org.huebert.ncbot.controller;

import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private static final int PAGE_SIZE = 25;

    private final ChatMessageRepository messageRepository;
    private final ChatChannelRepository channelRepository;
    private final ChatMemory2Repository memoryRepository;
    private final ChatParticipantRepository participantRepository;
    private final Instant startTime = Instant.now();

    public AdminApiController(ChatMessageRepository messageRepository,
                              ChatChannelRepository channelRepository,
                              ChatMemory2Repository memoryRepository,
                              ChatParticipantRepository participantRepository) {
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.memoryRepository = memoryRepository;
        this.participantRepository = participantRepository;
    }

    // ── 1.1 Dashboard / Channels ──

    @GetMapping("/channels")
    public Map<String, List<ChannelDto>> channels() {
        List<ChatChannel> publicChannels = channelRepository.findPublicChannels();
        List<ChatChannel> dmChannels = channelRepository.findDmChannels();

        return Map.of(
                "publicChannels", publicChannels.stream().map(ChannelDto::from).toList(),
                "dmChannels", dmChannels.stream().map(ChannelDto::from).toList()
        );
    }

    // ── 1.2 Messages ──

    @GetMapping("/messages/{channelId}")
    public MessagesResponse messages(@PathVariable Long channelId) {
        List<ChatMessage> messages = messageRepository.findMessagesByChannelOrderByCreatedDesc(channelId, PAGE_SIZE);
        return new MessagesResponse(channelId, null, messages.stream().map(MessageDto::from).toList());
    }

    @GetMapping("/messages/{channelId}/older")
    public MessagesResponse olderMessages(@PathVariable Long channelId,
                                          @RequestParam String before) {
        Instant beforeInstant;
        try {
            beforeInstant = Instant.parse(before);
        } catch (Exception e) {
            return new MessagesResponse(channelId, null, List.of());
        }

        List<ChatMessage> messages = messageRepository.findMessagesByChannelBefore(channelId, beforeInstant, PAGE_SIZE);
        return new MessagesResponse(channelId, null, messages.stream().map(MessageDto::from).toList());
    }

    // ── 1.3 Memory ──

    @GetMapping("/memory")
    public MemoryResponse memory(@RequestParam(required = false) Long channelId) {
        List<ChatMemory> memories;
        if (channelId == null) {
            memories = memoryRepository.findGlobalMemory();
        } else {
            memories = memoryRepository.findMemory(channelId);
        }
        return new MemoryResponse(memories.stream().map(MemoryDto::from).toList());
    }

    @GetMapping("/memory/global")
    public MemoryResponse globalMemory() {
        List<ChatMemory> memories = memoryRepository.findGlobalMemory();
        return new MemoryResponse(memories.stream().map(MemoryDto::from).toList());
    }

    @PostMapping("/memory")
    public MemoryDto createMemory(@RequestBody MemoryCreateRequest request) {
        ChatMemory memory = ChatMemory.builder()
                .chatChannelId(request.channelId())
                .key(request.key())
                .value(request.value())
                .build();
        memoryRepository.save(memory);
        return MemoryDto.from(memory);
    }

    @PutMapping("/memory/{id}")
    public MemoryDto updateMemory(@PathVariable Long id,
                                  @RequestBody MemoryUpdateRequest request) {
        ChatMemory memory = memoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + id));
        memory.setKey(request.key());
        memory.setValue(request.value());
        memoryRepository.save(memory);
        return MemoryDto.from(memory);
    }

    @DeleteMapping("/memory/{id}")
    public ResponseEntity<Void> deleteMemory(@PathVariable Long id) {
        memoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/memory/promote")
    public MemoryDto promoteMemory(@RequestBody MemoryPromoteRequest request) {
        ChatMemory source = memoryRepository.findById(request.memoryId())
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + request.memoryId()));

        ChatMemory promoted = ChatMemory.builder()
                .chatChannelId(null)
                .key(source.getKey())
                .value(source.getValue())
                .build();
        memoryRepository.save(promoted);
        return MemoryDto.from(promoted);
    }

    // ── 1.4 Participants ──

    @GetMapping("/participants")
    public ParticipantsResponse participants(@RequestParam(required = false) Long channelId) {
        if (channelId == null) {
            List<ChatParticipant> all = participantRepository.findLastSeen();
            return new ParticipantsResponse(all.stream()
                    .map(p -> new ParticipantDto(p.getName(), p.getLastSeen()))
                    .toList());
        } else {
            List<String> senders = messageRepository.findSenderNamesByChannel(channelId);
            return new ParticipantsResponse(senders.stream()
                    .map(name -> new ParticipantDto(name, null))
                    .toList());
        }
    }

    @GetMapping("/participants/all")
    public ParticipantsResponse allParticipants() {
        List<ChatParticipant> all = participantRepository.findLastSeen();
        return new ParticipantsResponse(all.stream()
                .map(p -> new ParticipantDto(p.getName(), p.getLastSeen()))
                .toList());
    }

    // ── 1.5 System Info ──

    @GetMapping("/info")
    public InfoResponse info() {
        long uptimeSeconds = Duration.between(startTime, Instant.now()).getSeconds();
        long totalMessages = messageRepository.count();
        long totalChannels = channelRepository.count();
        long totalMemories = memoryRepository.count();

        return new InfoResponse(uptimeSeconds, totalMessages, totalChannels, totalMemories);
    }

    // ── DTOs ──

    public record ChannelDto(Long id, String channelKey, String channelName, Boolean isDm) {
        static ChannelDto from(ChatChannel c) {
            return new ChannelDto(c.getId(), c.getChannelKey(), c.getChannelName(), c.getIsDm());
        }
    }

    public record MessagesResponse(Long channelId, String channelName, List<MessageDto> messages) {
        public MessagesResponse(Long channelId, String channelName, List<MessageDto> messages) {
            this.channelId = channelId;
            this.channelName = channelName != null ? channelName : (channelId != null ? "Channel #" + channelId : null);
            this.messages = messages;
        }
    }

    public record MessageDto(Long id, String senderName, String content, String createdAt) {
        static MessageDto from(ChatMessage m) {
            return new MessageDto(m.getId(), m.getSenderName(), m.getContent(), m.getCreatedAt().toString());
        }
    }

    public record MemoryResponse(List<MemoryDto> memories) {}

    public record MemoryDto(Long id, Long channelId, String key, String value) {
        static MemoryDto from(ChatMemory m) {
            return new MemoryDto(m.getId(), m.getChatChannelId(), m.getKey(), m.getValue());
        }
    }

    public record MemoryCreateRequest(Long channelId, String key, String value) {}

    public record MemoryUpdateRequest(String key, String value) {}

    public record MemoryPromoteRequest(Long memoryId) {}

    public record ParticipantsResponse(List<ParticipantDto> senders) {}

    public record ParticipantDto(String name, Instant lastSeen) {}

    public record InfoResponse(long uptimeSeconds, long totalMessages, long totalChannels, long totalMemories) {}
}
