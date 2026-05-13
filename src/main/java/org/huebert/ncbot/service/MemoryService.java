package org.huebert.ncbot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.entity.ConversationMemory;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ConversationMemoryRepository;
import org.huebert.ncbot.util.MessageFormatter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemoryService {

    private static final String COMBINE_PROMPT = """
            You are an expert at combining historical conversation memory with new information.
            You will be provided the historical conversation memory followed by a summary of new information.
            Your response should be an updated conversation memory that contains important information from both.
            You can assume the new information comes after the historical memory.
            The only time you should omit information is when it is no longer relevant or has been superceded by later information.
            You may return the historical conversation memory when nothing should be changed.
            """;

    private static final String MEMORY_PROMPT = """
            Your task is to summarize the provided conversation history.
            The summary that is produced will be used by ncbot as its long term memory.
            """;

    private final ChatMessageRepository chatMessageRepository;

    private final ConversationMemoryRepository conversationMemoryRepository;

    private final ChatClient chatClient;

    public MemoryService(ChatModel chatModel, ChatMessageRepository chatMessageRepository, ConversationMemoryRepository conversationMemoryRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationMemoryRepository = conversationMemoryRepository;
        this.chatClient = ChatClient.builder(chatModel)
                .build();
    }

    @Scheduled(cron = "0 0 3 * * *")
//    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void updateMemory() {

        for (String channelKey : chatMessageRepository.findDistinctChannelKeys()) {
            log.debug("channelKey: {}", channelKey);
            ConversationMemory memory = conversationMemoryRepository.findChannelMemory(channelKey).orElse(null);
            if (memory == null) {
                memory = ConversationMemory.builder()
                        .channelKey(channelKey)
                        .updatedAt(ZonedDateTime.now().minusYears(10).toInstant())
                        .content("")
                        .build();
            }
            List<ChatMessage> messages = chatMessageRepository.findLatestChannelMessages(channelKey, memory.getUpdatedAt());
            log.debug("message count: {}", messages.size());
            if (!messages.isEmpty()) {
                updateMemory(memory, messages);
            }
            conversationMemoryRepository.save(memory);
        }

        for (String senderKey : chatMessageRepository.findDistinctSenderKeys()) {
            log.debug("senderKey: {}", senderKey);
            ConversationMemory memory = conversationMemoryRepository.findDmMemory(senderKey).orElse(null);
            if (memory == null) {
                memory = ConversationMemory.builder()
                        .senderKey(senderKey)
                        .updatedAt(ZonedDateTime.now().minusYears(10).toInstant())
                        .content("")
                        .build();
            }
            List<ChatMessage> messages = chatMessageRepository.findLatestDms(senderKey, memory.getUpdatedAt());
            log.debug("message count: {}", messages.size());
            if (!messages.isEmpty()) {
                updateMemory(memory, messages);
            }
            conversationMemoryRepository.save(memory);
        }
    }

    private void updateMemory(ConversationMemory memory, List<ChatMessage> messages) {
        String additional = summarize(messages);
        String updated = combine(memory.getContent(), additional);
        if (!memory.getContent().equals(updated)) {
            memory.setContent(updated);
            memory.setUpdatedAt(Instant.now());
        }
    }

    private String summarize(List<ChatMessage> messages) {
        log.debug("summarize: {}", messages.size());

        String user = messages.stream()
                .map(MessageFormatter::buildUserMessage)
                .collect(Collectors.joining("\n"));
        log.debug("updateMemory user: {}", user);

        String response = chatClient.prompt()
                .system(MEMORY_PROMPT)
                .user(user)
                .call()
                .content();

        log.debug("summarize result: {}", response);
        return response;
    }

    private String combine(String current, String additional) {
        log.debug("combine: current={}, additional={}", current, additional);

        if (Strings.trimToNull(additional) == null) {
            log.debug("additional memory was empty");
            return current;
        }

        if (Strings.trimToNull(current) == null) {
            log.debug("current memory was empty");
            return additional;
        }

        String response = chatClient.prompt()
                .system(COMBINE_PROMPT)
                .user(String.format("""
                        ## Historical Memory
                        %s
                        ## New Information
                        %s
                        """, current, additional))
                .call()
                .content();

        log.debug("combine result: {}", response);
        return response;
    }

}
