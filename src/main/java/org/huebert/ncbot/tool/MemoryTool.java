package org.huebert.ncbot.tool;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Memory maintenance tools registered on both the live chat {@code ChatClient} and the
 * scheduled memory-synthesis {@code ChatClient}. The model calls these to read and mutate
 * channel memory directly, so the AI's written response no longer needs to be parsed.
 * <p>
 * The tools are stateless: every tool takes the numeric {@code channelId}
 * as an explicit argument (rendered in prompts as {@code CHANNEL_ID}). Scope rules:
 * <ul>
 *   <li>{@code insertMemory}/{@code updateMemory}/{@code deleteMemory}
 *       operate exclusively on <b>channel-scoped</b> memory for the given channel.</li>
 *   <li>Global memory is read-only and is never modified by these tools. A channel memory
 *       with the same key as a global one overrides it for that channel.</li>
 * </ul>
 * The current channel+global memories are always supplied inline to the model as
 * {@code __CHAT_MEMORY__} (batch synthesis and live chat), so no read tools are needed.
 * Writes persist through the Spring Data repository (transactional at the repository level);
 * the tool instance itself is invoked reflectively, so no method-level transaction is applied.
 */
@Slf4j
@Component
public class MemoryTool {

    /** DB column size for key/value (chat_memory.key_text / value_text are varchar(255)). */
    private static final int MAX_FIELD_LENGTH = 255;

    private final ChatMemory2Repository memoryRepository;
    private final ChatChannelRepository channelRepository;

    public MemoryTool(ChatMemory2Repository memoryRepository, ChatChannelRepository channelRepository) {
        this.memoryRepository = memoryRepository;
        this.channelRepository = channelRepository;
    }

    /** Structured result of a write operation, serialized to the model as JSON. */
    public record MemoryResult(String status, String message) {
    }

    @Tool(description = "Insert a NEW channel-scoped memory. Fails if a channel memory with the same key already "
            + "exists for that channel (use updateMemory instead). If a global memory has the same key, this creates a "
            + "channel-scoped override for that channel. Values are dense facts, not full sentences.")
    public MemoryResult insertMemory(
            @ToolParam(description = "Numeric id of the current channel, from the CHANNEL_ID line in the prompt") Long channelId,
            @ToolParam(description = "Dot-separated memory key, e.g. user.john.pref.color") String key,
            @ToolParam(description = "Dense factual value, e.g. blue (not 'the user likes the color blue')") String value) {
        String cause = validate(channelId, key, value);
        if (cause != null) {
            return new MemoryResult("error", cause);
        }
        if (memoryRepository.findChannelMemoryByKey(channelId, key).isPresent()) {
            return new MemoryResult("error", "channel memory '" + key + "' already exists for channel " + channelId
                    + "; use updateMemory instead");
        }
        memoryRepository.save(ChatMemory.builder()
                .chatChannelId(channelId)
                .key(key)
                .value(value)
                .build());
        return new MemoryResult("ok", "inserted channel memory '" + key + "'");
    }

    @Tool(description = "Update the value of an EXISTING channel-scoped memory. Fails if no channel memory with the "
            + "key exists for that channel (use insertMemory instead). Global memory can never be updated — to customize "
            + "a global fact for this channel, insert a channel memory with the same key.")
    public MemoryResult updateMemory(
            @ToolParam(description = "Numeric id of the current channel, from the CHANNEL_ID line in the prompt") Long channelId,
            @ToolParam(description = "Dot-separated memory key that already exists as a channel memory") String key,
            @ToolParam(description = "New dense factual value") String value) {
        String cause = validate(channelId, key, value);
        if (cause != null) {
            return new MemoryResult("error", cause);
        }
        ChatMemory memory = memoryRepository.findChannelMemoryByKey(channelId, key).orElse(null);
        if (memory == null) {
            return new MemoryResult("error", "no channel memory with key '" + key + "' for channel " + channelId
                    + "; use insertMemory to create it (this also overrides a global memory with the same key)");
        }
        memory.setValue(value);
        memoryRepository.save(memory);
        return new MemoryResult("ok", "updated channel memory '" + key + "'");
    }

    @Tool(description = "Delete an EXISTING channel-scoped memory. Fails if no channel memory with the key exists "
            + "for that channel. Only removes the channel-scoped memory; global memory is never touched (a global memory "
            + "with the same key remains effective in other channels).")
    public MemoryResult deleteMemory(
            @ToolParam(description = "Numeric id of the current channel, from the CHANNEL_ID line in the prompt") Long channelId,
            @ToolParam(description = "Dot-separated memory key that already exists as a channel memory") String key) {
        if (channelId == null) {
            return new MemoryResult("error", "channelId must not be null");
        }
        String cause = validateChannelAndKey(channelId, key);
        if (cause != null) {
            return new MemoryResult("error", cause);
        }
        ChatMemory memory = memoryRepository.findChannelMemoryByKey(channelId, key).orElse(null);
        if (memory == null) {
            return new MemoryResult("error", "no channel memory with key '" + key + "' for channel " + channelId + " to delete");
        }
        memoryRepository.delete(memory);
        return new MemoryResult("ok", "deleted channel memory '" + key + "'");
    }

    private String validate(Long channelId, String key, String value) {
        String cause = validateChannelAndKey(channelId, key);
        if (cause != null) {
            return cause;
        }
        if (value == null || value.isBlank()) {
            return "value must be non-blank";
        }
        if (value.length() > MAX_FIELD_LENGTH) {
            return "value must be at most " + MAX_FIELD_LENGTH + " characters";
        }
        return null;
    }

    private String validateChannelAndKey(Long channelId, String key) {
        if (channelId == null) {
            return "channelId must not be null";
        }
        if (!channelRepository.existsById(channelId)) {
            return "no channel with id " + channelId;
        }
        if (key == null || key.isBlank()) {
            return "key must be non-blank";
        }
        if (key.length() > MAX_FIELD_LENGTH) {
            return "key must be at most " + MAX_FIELD_LENGTH + " characters";
        }
        return null;
    }

}