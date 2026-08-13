package org.huebert.ncbot;

import org.huebert.ncbot.controller.dto.ChannelDto;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.dto.PromptMessage;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.service.ChannelService;
import org.huebert.ncbot.service.MemoryService;
import org.huebert.ncbot.service.MessageService;
import org.huebert.ncbot.service.ParticipantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SearchTest {

    @Autowired
    private ChannelService channelService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ChatChannelRepository channelRepository;

    @Autowired
    private ChatMemory2Repository memoryRepository;

    @Autowired
    private ChatParticipantRepository participantRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private MessageService messageService;

    private ChatChannel saveChannel(String name) {
        return channelRepository.save(ChatChannel.builder()
                .channelKey(name)
                .channelName(name)
                .isDm(false)
                .memoryUpdatedAt(Instant.EPOCH)
                .build());
    }

    @Test
    void channelSearchMatchesNameAndKeyCaseInsensitively() {
        // Unique token so the shared dev database cannot interfere
        String token = "srch" + System.nanoTime();
        saveChannel("#" + token); // matches by name
        channelRepository.save(ChatChannel.builder()
                .channelKey(token + "-key")
                .channelName("other-" + System.nanoTime())
                .isDm(false)
                .memoryUpdatedAt(Instant.EPOCH)
                .build()); // matches by key only

        Page<ChannelDto> result = channelService.findChannels(null, token.toUpperCase(), PageRequest.of(0, 25));
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(c ->
                c.channelName().toLowerCase().contains(token)
                        || c.channelKey().toLowerCase().contains(token)));
    }

    @Test
    void channelSearchWithoutQueryReturnsAll() {
        saveChannel("#alpha-" + System.nanoTime());
        Page<ChannelDto> result = channelService.findChannels(null, null, PageRequest.of(0, 100));
        assertTrue(result.getTotalElements() >= 1);
    }

    @Test
    void globalMemorySearchMatchesKeyOrValueCaseInsensitively() {
        String token = "mem" + System.nanoTime();
        memoryRepository.save(ChatMemory.builder()
                .chatChannelId(null)
                .key("user." + token)
                .value("Alice loves " + token + " sailing")
                .build());
        memoryRepository.save(ChatMemory.builder()
                .chatChannelId(null)
                .key("bot.version")
                .value("v2.3.1")
                .build());

        Page<ChatMemory> byKey = memoryService.findGlobalMemory("USER." + token.toUpperCase(), PageRequest.of(0, 25));
        assertEquals(1, byKey.getTotalElements());
        assertEquals("user." + token, byKey.getContent().get(0).getKey());

        Page<ChatMemory> byValue = memoryService.findGlobalMemory(token, PageRequest.of(0, 25));
        assertEquals(1, byValue.getTotalElements());
        assertEquals("user." + token, byValue.getContent().get(0).getKey());
    }

    @Test
    void channelMemorySearchScopesToChannel() {
        String token = "mem" + System.nanoTime();
        ChatChannel channel = saveChannel("#mem-" + token);
        memoryRepository.save(ChatMemory.builder()
                .chatChannelId(channel.getId())
                .key("channel." + token)
                .value("Bot development " + token)
                .build());
        memoryRepository.save(ChatMemory.builder()
                .chatChannelId(null)
                .key("channel." + token)
                .value("Global topic " + token)
                .build());

        Page<ChatMemory> result = memoryService.findChannelMemory(channel.getId(), token, PageRequest.of(0, 25));
        assertEquals(1, result.getTotalElements());
        assertEquals(channel.getId(), result.getContent().get(0).getChatChannelId());
    }

    @Test
    void participantSearchMatchesNameCaseInsensitively() {
        String token = "part" + System.nanoTime();
        Instant now = Instant.now();
        participantRepository.save(ChatParticipant.builder()
                .name(token + "-Alice")
                .firstSeen(now)
                .lastSeen(now)
                .build());
        participantRepository.save(ChatParticipant.builder()
                .name(token + "-Bob")
                .firstSeen(now)
                .lastSeen(now)
                .build());

        Page<ChatParticipant> result = participantService.findAllParticipants(token + "-ALICE", PageRequest.of(0, 25));
        assertEquals(1, result.getTotalElements());
        assertEquals(token + "-Alice", result.getContent().get(0).getName());

        Page<ChatParticipant> none = participantService.findAllParticipants(token + "-nobody", PageRequest.of(0, 25));
        assertEquals(0, none.getTotalElements());
    }

    @Test
    void participantSearchWithoutQueryReturnsAll() {
        Instant now = Instant.now();
        participantRepository.save(ChatParticipant.builder()
                .name("search-all-" + System.nanoTime())
                .firstSeen(now)
                .lastSeen(now)
                .build());

        Page<ChatParticipant> result = participantService.findAllParticipants(null, PageRequest.of(0, 100));
        assertTrue(result.getTotalElements() >= 1);
    }

    @Test
    void channelParticipantSearchFiltersSenderNames() {
        String token = "sender" + System.nanoTime();
        String nameA = token + "-alice";
        String nameB = token + "-bob";
        ChatChannel channel = saveChannel("#cp-" + token);
        Instant now = Instant.now();
        for (String name : new String[]{nameA, nameB}) {
            messageRepository.save(ChatMessage.builder()
                    .chatChannelId(channel.getId())
                    .senderName(name)
                    .content("hello")
                    .createdAt(now)
                    .build());
            participantRepository.save(ChatParticipant.builder()
                    .name(name)
                    .firstSeen(now)
                    .lastSeen(now)
                    .build());
        }

        Page<ChatParticipant> result = participantService.findParticipantsByChannel(channel.getId(), token + "-ALICE", PageRequest.of(0, 25));
        assertEquals(1, result.getTotalElements());
        assertEquals(nameA, result.getContent().get(0).getName());

        Page<ChatParticipant> all = participantService.findParticipantsByChannel(channel.getId(), null, PageRequest.of(0, 25));
        assertEquals(2, all.getTotalElements());
    }

    @Test
    void historySearchFiltersByTermAndSenderOldestFirst() {
        String token = "hist" + System.nanoTime();
        ChatChannel channel = saveChannel("#hist-" + token);
        Instant t0 = Instant.now().minusSeconds(100);
        Instant t1 = t0.plusSeconds(10);
        Instant t2 = t0.plusSeconds(20);
        messageRepository.save(ChatMessage.builder()
                .chatChannelId(channel.getId()).senderName("alice").content("alpha " + token).createdAt(t0).build());
        messageRepository.save(ChatMessage.builder()
                .chatChannelId(channel.getId()).senderName("bob").content("unrelated note").createdAt(t1).build());
        messageRepository.save(ChatMessage.builder()
                .chatChannelId(channel.getId()).senderName("alice").content("beta " + token + " twice").createdAt(t2).build());

        List<PromptMessage> byTerm = messageService.searchHistory(
                channel.getId(), null, null, null, token.toUpperCase(), 50);
        assertEquals(2, byTerm.size());
        assertEquals("alpha " + token, byTerm.get(0).message());
        assertEquals("beta " + token + " twice", byTerm.get(1).message());
        assertTrue(byTerm.stream().allMatch(m -> m.sender().equals("alice")));

        List<PromptMessage> bySender = messageService.searchHistory(
                channel.getId(), null, null, "bob", null, 50);
        assertEquals(1, bySender.size());
        assertEquals("unrelated note", bySender.get(0).message());

        List<PromptMessage> after = messageService.searchHistory(
                channel.getId(), t0, null, null, null, 50);
        assertEquals(2, after.size());
        assertEquals("beta " + token + " twice", after.get(1).message());

        List<PromptMessage> limited = messageService.searchHistory(
                channel.getId(), null, null, null, null, 1);
        assertEquals(1, limited.size());
        assertEquals("beta " + token + " twice", limited.get(0).message());
    }

    @Test
    void channelParticipantsToolReturnsChannelSendersNewestFirst() {
        String token = "ptool" + System.nanoTime();
        ChatChannel channel = saveChannel("#ptool-" + token);
        Instant now = Instant.now();
        for (String name : new String[]{"ptool-" + token + "-alice", "ptool-" + token + "-bob"}) {
            messageRepository.save(ChatMessage.builder()
                    .chatChannelId(channel.getId()).senderName(name).content("hi").createdAt(now).build());
            participantRepository.save(ChatParticipant.builder().name(name).firstSeen(now).lastSeen(now).build());
        }

        List<ChatParticipant> participants = participantService.findChannelParticipants(channel.getId(), 25);
        assertEquals(2, participants.size());
        assertTrue(participants.stream().allMatch(p -> p.getName().startsWith("ptool-" + token)));
    }

}
