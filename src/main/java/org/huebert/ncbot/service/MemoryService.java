package org.huebert.ncbot.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.AiMode;
import org.huebert.ncbot.config.ChannelCapabilities;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemoryService {

    private static final String DELETE_VALUE = "__DELETE__";

    private final NcbotProperties ncbotProperties;
    private final ChatClient chatClient;
    private final TemplateService templateService;
    private final ChatChannelRepository chatChannelRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemory2Repository chatMemoryRepository;

    public MemoryService(
            NcbotProperties ncbotProperties,
            ChatChannelRepository chatChannelRepository,
            ChatModel chatModel,
            ChatMessageRepository chatMessageRepository,
            ChatMemory2Repository chatMemoryRepository,
            TemplateService templateService
    ) {
        this.ncbotProperties = ncbotProperties;
        this.chatChannelRepository = chatChannelRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMemoryRepository = chatMemoryRepository;
        this.templateService = templateService;
        this.chatClient = ChatClient.builder(chatModel)
                .build();
    }

    @Scheduled(fixedDelayString = "${ncbot.memory-update-period}")
    public void updateMemory() {
        log.debug("updateMemory: starting");
        Instant now = Instant.now();
        long start = System.currentTimeMillis();

        for (ChatChannel channel : chatChannelRepository.findAll()) {
            log.debug("channel: {}", channel);

            if (!channel.getIsDm()) {
                ChannelCapabilities caps = ChannelCapabilities.from(channel.getChannelName(), ncbotProperties);
                if (caps.ai() == AiMode.DISABLED) {
                    log.debug("ai disabled for channel {}, skipping", channel.getChannelName());
                    continue;
                }
            }

            List<ChatMessage> messages = chatMessageRepository.findChannelMessages(channel.getId(), channel.getMemoryUpdatedAt(), now);
            log.debug("messages count: {}", messages.size());
            if (messages.isEmpty()) {
                continue;
            }

            for (List<ChatMessage> partition : Lists.partition(messages, ncbotProperties.memoryPartitionSize())) {
                updateMemory(channel, partition);
            }

            channel.setMemoryUpdatedAt(now);
            chatChannelRepository.save(channel);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("updateMemory completed in {} ms", elapsed);
    }

    private void updateMemory(ChatChannel channel, List<ChatMessage> messages) {
        log.debug("updateMemory: channel={}, messages size={}", channel, messages.size());

        Map<String, ChatMemory> memories = chatMemoryRepository.findMemory(channel.getId()).stream()
                .collect(Collectors.toMap(ChatMemory::getKey, a -> a));
        log.debug("memories count: {}", memories.size());

        for (Map.Entry<String, String> entry : getUpdates(memories, messages).entrySet()) {
            ChatMemory memory = memories.get(entry.getKey());
            if (memory == null) {
                if (!DELETE_VALUE.equals(entry.getValue())) {
                    // Added
                    log.debug("adding memory: {}", entry);
                    chatMemoryRepository.save(ChatMemory.builder()
                            .chatChannelId(channel.getId())
                            .key(entry.getKey())
                            .value(entry.getValue())
                            .build());
                }
            } else if (memory.getChatChannelId() != null) {
                if (DELETE_VALUE.equals(entry.getValue())) {
                    // Deleted
                    log.debug("deleting memory: {}", entry);
                    chatMemoryRepository.delete(memory);
                } else {
                    // Updated
                    log.debug("updating memory: {}", entry);
                    memory.setValue(entry.getValue());
                    chatMemoryRepository.save(memory);
                }
            }
        }
    }

    private Map<String, String> getUpdates(Map<String, ChatMemory> existingMemories, List<ChatMessage> messages) {
        log.debug("getUpdates: memories size={}, messages size={}", existingMemories.size(), messages.size());

        String user = templateService.render("memory", Map.of(
                "memories", existingMemories.values(),
                "messages", messages
        ));
        log.debug("getUpdates: user={}", user);

        String response = chatClient.prompt()
                .system(ncbotProperties.memoryPrompt())
                .user(user)
                .call()
                .content();

        if ("EMPTY".equalsIgnoreCase(Strings.trimToNull(response))) {
            log.debug("getUpdates result: EMPTY");
            return Map.of();
        }

        Map<String, String> result = new HashMap<>();
        for (String line : response.split("\n")) {
            if (!line.contains("=")) {
                continue;
            }
            String[] values = line.split("=");
            result.put(values[0].trim(), values[1].trim());
        }

        log.debug("getUpdates result: {}", result);
        return result;
    }

}
