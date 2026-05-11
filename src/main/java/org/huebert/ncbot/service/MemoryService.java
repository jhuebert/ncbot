package org.huebert.ncbot.service;

import lombok.extern.slf4j.Slf4j;
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

    private static final String MEMORY_PROMPT = """
            You are a conversation memory system for ncbot which is a Meshcore AI assistant.
            Your task is to produce a summary of a conversation that will be stored and used as long-term context for future responses.
            Information is progressively accumulated in the conversation memory over time by including the information from the latest messages.
            You will be provided the current memory contents in addition to chat messages.
            The chat message contents should be incorporated into the memory.
            Your response should be the updated conversation memory contents.
            You may return the current conversation memory when nothing should be changed.
            
            ## ALWAYS preserve
            - Important facts (things that are not commonly known)
            - Significant decisions that may need to be recalled later
            - Unresolved problems that may resurface
            - Information about users to help ncbot better respond to and about individual users
              - name
              - personal preferences
              - personality
              - characteristics about who they are
            - Key topics and conclusions
            - Notable events that occurred in the past or are upcoming
            - Recurring themes, memes, or jokes.
              - ncbot can use these to more convincingly appear as a member of the inner circle
            
            ## ALWAYS discard
            - Greetings, pleasantries, acknowledgments
            - Redundant or repeated statements
            - Anything superseded by a later statement
            - Conversational filler that carries no information
            
            ## OUTPUT FORMAT:
            - Write the minimum number of words needed to capture what matters.
            - Prefer short bullet points for specific details or lists
            - Prefer tight prose organized by theme to cover topical information
            - Do not pad, explain your reasoning, or reference these instructions.
            - Do not invent anything not present in the source material.
            """;

    private static final String USER_PROMPT = """
            ## Current Memory
            
            %s
            
            ## Messages to Update Memory
            
            %s
            """;

    private final ChatMessageRepository chatMessageRepository;

    private final ConversationMemoryRepository conversationMemoryRepository;

    private final ChatClient chatClient;

    public MemoryService(ChatModel chatModel, ChatMessageRepository chatMessageRepository, ConversationMemoryRepository conversationMemoryRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationMemoryRepository = conversationMemoryRepository;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(MEMORY_PROMPT)
                .build();
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void updateMemory() {

//        conversationMemoryRepository.deleteAll();

        for (String channelKey : chatMessageRepository.findDistinctChannelKeys()) {
            log.debug("channelKey: {}", channelKey);
            ConversationMemory memory = conversationMemoryRepository.findChannelMemory(channelKey).orElse(null);
            if (memory == null) {
                memory = ConversationMemory.builder()
                        .channelKey(channelKey)
                        .updatedAt(ZonedDateTime.now().minusYears(1).toInstant())
                        .content("")
                        .build();
            }
            List<ChatMessage> messages = chatMessageRepository.findLatestChannelMessages(channelKey, memory.getUpdatedAt());
            log.debug("message count: {}", messages.size());
            if (!messages.isEmpty()) {
                updateMemory(memory, messages);
                conversationMemoryRepository.save(memory);
            }
        }

        for (String senderKey : chatMessageRepository.findDistinctSenderKeys()) {
            log.debug("senderKey: {}", senderKey);
            ConversationMemory memory = conversationMemoryRepository.findDmMemory(senderKey).orElse(null);
            if (memory == null) {
                memory = ConversationMemory.builder()
                        .senderKey(senderKey)
                        .updatedAt(ZonedDateTime.now().minusYears(1).toInstant())
                        .content("")
                        .build();
            }
            List<ChatMessage> messages = chatMessageRepository.findLatestDms(senderKey, memory.getUpdatedAt());
            log.debug("message count: {}", messages.size());
            if (!messages.isEmpty()) {
                updateMemory(memory, messages);
                conversationMemoryRepository.save(memory);
            }
        }
    }

    private void updateMemory(ConversationMemory memory, List<ChatMessage> messages) {
        log.debug("updateMemory: {}", memory);

        String user = String.format(USER_PROMPT, memory.getContent(), messages.stream()
                .map(MessageFormatter::buildUserMessage)
                .collect(Collectors.joining("\n")));

        log.debug("updateMemory user: {}", user);

        String response = chatClient.prompt()
                .user(user)
                .call()
                .content();

        log.debug("updateMemory result: {}", response);
        if (!memory.getContent().equals(response)) {
            memory.setContent(response);
            memory.setUpdatedAt(Instant.now());
        }

    }

}
