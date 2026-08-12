package org.huebert.ncbot.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.AiMode;
import org.huebert.ncbot.config.ChannelCapabilities;
import org.huebert.ncbot.dto.PromptMessage;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMemoryFailure;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.controller.dto.MemoryFailureDto;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMemoryFailureRepository;
import org.huebert.ncbot.tool.MemoryTool;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemoryService {

    /**
     * Number of attempts for the memory-synthesis AI call. Transient provider resets
     * (e.g. an HTTP/2 stream CANCEL or a brief timeout) can abort a call, so retry a
     * couple of times before giving up on a partition.
     */
    private static final int MEMORY_CALL_ATTEMPTS = 3;

    /** OpenAI finish reason emitted when provider content moderation refuses a prompt. */
    private static final String FINISH_REASON_CONTENT_FILTER = "content_filter";

    private static final int FAILURE_ERROR_MAX_LENGTH = 2000;

    private final ConfigService configService;
    private final ChatClient chatClient;
    private final TemplateService templateService;
    private final ChannelService channelService;
    private final MessageService messageService;
    private final ChatMemory2Repository chatMemoryRepository;
    private final ChatMemoryFailureRepository chatMemoryFailureRepository;
    private final MemoryTool memoryTool;

    /**
     * Consecutive failed synthesis runs per channel. A partition that fails this many times
     * in a row is skipped (cursor advanced past it) so a deterministically failing batch —
     * e.g. provider content moderation on a batch of messages — cannot wedge the channel by
     * being retried forever.
     */
    private final Map<Long, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    public MemoryService(
            ConfigService configService,
            ChatModel chatModel,
            ChatMemory2Repository chatMemoryRepository,
            ChatMemoryFailureRepository chatMemoryFailureRepository,
            TemplateService templateService,
            ChannelService channelService,
            MessageService messageService,
            MemoryTool memoryTool
    ) {
        this.configService = configService;
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatMemoryFailureRepository = chatMemoryFailureRepository;
        this.templateService = templateService;
        this.chatClient = ChatClient.builder(chatModel)
                .build();
        this.channelService = channelService;
        this.messageService = messageService;
        this.memoryTool = memoryTool;
    }

    // ── CRUD operations ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ChatMemory> findChannelMemory(Long channelId, String query, Pageable pageable) {
        String q = normalized(query);
        return q == null
                ? chatMemoryRepository.findChannelMemory(channelId, pageable)
                : chatMemoryRepository.searchChannelMemory(channelId, q, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChatMemory> findGlobalMemory(String query, Pageable pageable) {
        String q = normalized(query);
        return q == null
                ? chatMemoryRepository.findGlobalMemory(pageable)
                : chatMemoryRepository.searchGlobalMemory(q, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MemoryFailureDto> findRecentFailures(Pageable pageable) {
        Map<Long, String> channelNames = channelService.findAll().stream()
                .collect(Collectors.toMap(ChatChannel::getId, ChatChannel::getChannelName, (a, b) -> a));
        return chatMemoryFailureRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(f -> MemoryFailureDto.from(f, channelNames.get(f.getChatChannelId())));
    }

    @Transactional(readOnly = true)
    public Page<MemoryFailureDto> findChannelFailures(Long channelId, Pageable pageable) {
        String channelName = channelService.getChannel(channelId).channelName();
        return chatMemoryFailureRepository.findAllByChatChannelIdOrderByCreatedAtDesc(channelId, pageable)
                .map(f -> MemoryFailureDto.from(f, channelName));
    }

    /**
     * Re-process a skipped batch: rewind the channel's memory cursor to just before the
     * first message of the failed partition and clear the failure record so the next
     * scheduled run synthesizes it again. The retry succeeds even if the batch fails
     * again — it simply re-records a failure — so an admin can iterate until the cause
     * (e.g. a prompt change) is resolved.
     */
    @Transactional
    public void retryFailure(Long id) {
        ChatMemoryFailure failure = chatMemoryFailureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory failure not found: " + id));
        Long fromMessageId = failure.getFromMessageId();
        Instant fromCreatedAt = fromMessageId == null
                ? null
                : messageService.findCreatedAt(fromMessageId).orElse(null);
        if (fromCreatedAt == null) {
            throw new IllegalArgumentException(
                    "Cannot retry: the originating message (id " + fromMessageId + ") no longer exists");
        }
        // Cursor must be strictly before the first message so createdAt > cursor includes it.
        channelService.setMemoryUpdated(failure.getChatChannelId(), fromCreatedAt.minusNanos(1));
        consecutiveFailures.remove(failure.getChatChannelId());
        chatMemoryFailureRepository.delete(failure);
    }

    @Transactional
    public ChatMemory createChannelMemory(Long channelId, String key, String value) {
        return chatMemoryRepository.save(ChatMemory.builder()
                .chatChannelId(channelId)
                .key(key)
                .value(value)
                .build());
    }

    @Transactional
    public ChatMemory createGlobalMemory(String key, String value) {
        return chatMemoryRepository.save(ChatMemory.builder()
                .chatChannelId(null)
                .key(key)
                .value(value)
                .build());
    }

    @Transactional
    public ChatMemory updateChannelMemory(Long channelId, Long id, String key, String value) {
        ChatMemory memory = requireChannelMemory(channelId, id);
        memory.setKey(key);
        memory.setValue(value);
        return chatMemoryRepository.save(memory);
    }

    @Transactional
    public ChatMemory updateGlobalMemory(Long id, String key, String value) {
        ChatMemory memory = requireGlobalMemory(id);
        memory.setKey(key);
        memory.setValue(value);
        return chatMemoryRepository.save(memory);
    }

    @Transactional
    public void deleteChannelMemory(Long channelId, Long id) {
        chatMemoryRepository.delete(requireChannelMemory(channelId, id));
    }

    @Transactional
    public void deleteGlobalMemory(Long id) {
        chatMemoryRepository.delete(requireGlobalMemory(id));
    }

    @Transactional
    public ChatMemory promoteMemory(Long channelId, Long id) {
        ChatMemory source = requireChannelMemory(channelId, id);
        ChatMemory promoted = ChatMemory.builder()
                .chatChannelId(null)
                .key(source.getKey())
                .value(source.getValue())
                .build();
        chatMemoryRepository.save(promoted);
        chatMemoryRepository.delete(source);
        return promoted;
    }

    private ChatMemory requireChannelMemory(Long channelId, Long id) {
        ChatMemory memory = chatMemoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + id));
        if (!channelId.equals(memory.getChatChannelId())) {
            throw new IllegalArgumentException("Memory does not belong to channel " + channelId);
        }
        return memory;
    }

    private ChatMemory requireGlobalMemory(Long id) {
        ChatMemory memory = chatMemoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + id));
        if (memory.getChatChannelId() != null) {
            throw new IllegalArgumentException("Memory is not global");
        }
        return memory;
    }

    private static String normalized(String query) {
        return query == null ? null : query.trim();
    }

    // ── Scheduled memory synthesis ───────────────────────────────────

    @Scheduled(fixedDelayString = "${ncbot.memory-update-period}")
    @DebugLog
    public void updateMemory() {

        if (!configService.useMemory() || !configService.autoUpdateMemory()) {
            log.debug("memory update disabled");
            return;
        }
        Instant now = Instant.now();

        for (ChatChannel channel : channelService.findAll()) {
            try {
                updateChannel(channel, now);
            } catch (RuntimeException e) {
                // Isolate failures: one channel's AI timeout must not abort memory
                // updates for the remaining channels. The failing channel's cursor was
                // already advanced past every successful partition, so the next run
                // resumes from the failed partition instead of reprocessing the channel.
                log.error("memory update failed for channel {} ({}); will resume from last successful partition next cycle: {}",
                        channel.getId(), channel.getChannelName(), e.getMessage(), e);
            }
        }
    }

    private void updateChannel(ChatChannel channel, Instant now) {
        log.debug("channel: {}", channel);

        if (!channel.getIsDm()) {
            ChannelCapabilities caps = configService.getChannelCapabilities(channel.getChannelName());
            if (caps.ai() == AiMode.DISABLED) {
                log.debug("ai disabled for channel {}, skipping", channel.getChannelName());
                return;
            }
        }

        List<ChatMessage> messages = messageService.findChannelMessages(
                channel.getId(), channel.getMemoryUpdatedAt(), now);
        log.debug("messages count: {}", messages.size());
        if (messages.isEmpty()) {
            return;
        }

        List<List<ChatMessage>> partitions = Lists.partition(messages, configService.memoryPartitionSize());
        for (List<ChatMessage> partition : partitions) {
            Instant cursor = partition.getLast().getCreatedAt();
            Long fromId = partition.getFirst().getId();
            Long toId = partition.getLast().getId();
            try {
                updateMemory(channel, partition);
                consecutiveFailures.remove(channel.getId());
            } catch (RuntimeException e) {
                int failures = consecutiveFailures.merge(channel.getId(), 1, Integer::sum);
                if (failures < configService.memoryMaxFailures()) {
                    // Leave the cursor untouched so the same partition is retried next cycle.
                    log.warn("memory synthesis failed for channel {} ({}), partition msgs {}..{} (failure {}/{}); will retry next cycle: {}",
                            channel.getId(), channel.getChannelName(), fromId, toId,
                            failures, configService.memoryMaxFailures(), e.getMessage());
                    throw e;
                }
                // Poison partition: the AI call deterministically fails for this batch (e.g.
                // provider content moderation on the messages). Advance the cursor past it so
                // the channel is not stuck retrying the same batch forever, and record the skip.
                consecutiveFailures.remove(channel.getId());
                log.error("memory synthesis failed {} consecutive times for channel {} ({}); SKIPPING partition msgs {}..{} to avoid endless retries: {}",
                        configService.memoryMaxFailures(), channel.getId(), channel.getChannelName(),
                        fromId, toId, e.getMessage(), e);
                recordFailure(channel.getId(), fromId, toId, e.getMessage());
            }
            // Success (or a poisoned skip) advances the cursor; the retry path above does not.
            channelService.setMemoryUpdated(channel.getId(), cursor);
        }
    }

    private void updateMemory(ChatChannel channel, List<ChatMessage> messages) {
        log.debug("updateMemory: channel={}, messages size={}", channel.getId(), messages.size());

        // Existing channel+global memories are rendered inline (__CHAT_MEMORY__) so the model
        // does not need a guaranteed get-* tool call; it persists changes directly through the
        // MemoryTool, so the returned text is ignored and no parsing is needed.
        List<ChatMemory> memories = chatMemoryRepository.findMemory(channel.getId());
        callMemory(templateService.render("memory", Map.of(
                "channelId", channel.getId(),
                "memories", memories,
                "messages", messages.stream().map(PromptMessage::from).collect(Collectors.toList())
        )));
    }

    /**
     * Run the memory-synthesis AI call with a small bounded retry. The model makes
     * {@link MemoryTool} calls that write channel memory directly. A provider
     * content-moderation refusal (OpenAI finish_reason=content_filter) is treated as a
     * failure so the poison logic can skip the batch instead of silently dropping it.
     */
    private void callMemory(String user) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MEMORY_CALL_ATTEMPTS; attempt++) {
            try {
                ChatResponse response = chatClient.prompt()
                        .system(configService.memoryPrompt())
                        .user(user)
                        .tools(memoryTool)
                        .call()
                        .chatResponse();
                if (isContentFilterRefusal(response)) {
                    throw new IllegalStateException(
                            "AI provider refused the memory-synthesis request (content_filter); no memory changes were applied");
                }
                return;
            } catch (RuntimeException e) {
                last = e;
                log.warn("memory synthesis AI call failed (attempt {}/{}): {}",
                        attempt, MEMORY_CALL_ATTEMPTS, e.getMessage());
            }
        }
        throw last;
    }

    private static boolean isContentFilterRefusal(ChatResponse response) {
        return response.getResults().stream()
                .map(Generation::getMetadata)
                .map(ChatGenerationMetadata::getFinishReason)
                .anyMatch(FINISH_REASON_CONTENT_FILTER::equalsIgnoreCase);
    }

    private void recordFailure(Long channelId, Long fromId, Long toId, String error) {
        try {
            chatMemoryFailureRepository.save(ChatMemoryFailure.builder()
                    .chatChannelId(channelId)
                    .fromMessageId(fromId)
                    .toMessageId(toId)
                    .error(error == null ? "" : truncate(error, FAILURE_ERROR_MAX_LENGTH))
                    .createdAt(Instant.now())
                    .build());
        } catch (RuntimeException e) {
            log.error("failed to record memory failure for channel {}: {}", channelId, e.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

}
