package org.huebert.ncbot.controller;

import gg.jte.TemplateEngine;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatMemory;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMemory2Repository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int PAGE_SIZE = 25;

    private final ChatMessageRepository messageRepository;
    private final ChatChannelRepository channelRepository;
    private final ChatMemory2Repository memoryRepository;
    private final ChatParticipantRepository participantRepository;
    private final TemplateEngine templateEngine;

    public AdminController(ChatMessageRepository messageRepository,
                           ChatChannelRepository channelRepository,
                           ChatMemory2Repository memoryRepository,
                           ChatParticipantRepository participantRepository,
                           TemplateEngine templateEngine) {
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.memoryRepository = memoryRepository;
        this.participantRepository = participantRepository;
        this.templateEngine = templateEngine;
    }

    // ── Dashboard (full page) ──

    @GetMapping
    public String dashboard(Model model) {
        List<ChatChannel> publicChannels = channelRepository.findPublicChannels();
        List<ChatChannel> dmChannels = channelRepository.findDmChannels();

        // Load the Public channel's messages by default
        Optional<ChatChannel> publicChannel = channelRepository.findPublicChannel();

        List<ChatMessage> messages = List.of();
        Long channelId = null;
        String channelName = "No channels";

        if (publicChannel.isPresent()) {
            messages = messageRepository.findMessagesByChannelOrderByCreatedDesc(
                    publicChannel.get().getId(), PAGE_SIZE);
            channelId = publicChannel.get().getId();
            channelName = publicChannel.get().getChannelName();
        }

        // Pre-render messages HTML for initial page load
        String fmt = formatRelative(Instant.now());

        model.addAttribute("publicChannels", publicChannels);
        model.addAttribute("dmChannels", dmChannels);
        model.addAttribute("messages", messages);
        model.addAttribute("formatRelative", fmt);
        model.addAttribute("channelId", channelId);
        model.addAttribute("channelName", channelName);

        return "admin/index";
    }

    // ── Messages ──

    @GetMapping("/messages/{id}")
    @ResponseBody
    public String messagesHtml(@PathVariable Long id, Model model) {
        List<ChatMessage> messages = messageRepository.findMessagesByChannelOrderByCreatedDesc(id, PAGE_SIZE);
        String fmt = formatRelative(Instant.now());
        model.addAttribute("messages", messages);
        model.addAttribute("channelId", id);
        model.addAttribute("formatRelative", fmt);
        return "admin/_messages_list";
    }

    @GetMapping("/messages/{id}/older")
    @ResponseBody
    public String olderMessages(@PathVariable Long id,
                                @RequestParam(required = false) String before, Model model) {
        if (before == null) {
            return "";
        }

        Instant beforeInstant;
        try {
            beforeInstant = Instant.parse(before);
        } catch (Exception e) {
            return "";
        }

        List<ChatMessage> messages = messageRepository.findMessagesByChannelBefore(id, beforeInstant, PAGE_SIZE);
        if (messages.isEmpty()) {
            return "";
        }

        String fmt = formatRelative(beforeInstant);
        model.addAttribute("messages", messages);
        model.addAttribute("formatRelative", fmt);
        return "admin/_messages_list_older";
    }

    // ── Memory ──

    @GetMapping("/memory/{id}")
    @ResponseBody
    public String memoryHtml(@PathVariable Long id, Model model) {
        List<ChatMemory> memories = memoryRepository.findMemory(id);
        model.addAttribute("memories", memories);
        model.addAttribute("title", "Channel Memory");
        model.addAttribute("channelId", id);
        model.addAttribute("listId", "memory-list-" + id);
        return "admin/_memory_list";
    }

    @GetMapping("/memory/global")
    @ResponseBody
    public String globalMemoryHtml(Model model) {
        List<ChatMemory> memories = memoryRepository.findGlobalMemory();
        model.addAttribute("memories", memories);
        model.addAttribute("title", "Global Memory");
        model.addAttribute("channelId", null);
        model.addAttribute("listId", "global-memory-list");
        return "admin/_memory_list";
    }

    @PostMapping("/memory")
    @ResponseBody
    public String addMemory(@RequestParam Long channelId,
                            @RequestParam String key,
                            @RequestParam String value,
                            Model model) {
        ChatMemory memory = ChatMemory.builder()
                .chatChannelId(channelId)
                .key(key)
                .value(value)
                .build();
        memoryRepository.save(memory);
        model.addAttribute("memory", memory);
        model.addAttribute("channelId", channelId);
        return "admin/_memory_item";
    }

    @PutMapping("/memory/{id}")
    @ResponseBody
    public String updateMemory(
            @PathVariable Long id,
            @RequestParam String key,
            @RequestParam String value,
            Model model
    ) {
        ChatMemory memory = memoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + id));
        memory.setKey(key);
        memory.setValue(value);
        memoryRepository.save(memory);

        Long channelId = memory.getChatChannelId();
        model.addAttribute("memory", memory);
        model.addAttribute("channelId", channelId);
        return "admin/_memory_item";
    }

    @DeleteMapping("/memory/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteMemory(@PathVariable Long id) {
        memoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/memory/promote")
    @ResponseBody
    public String promoteMemory(@RequestParam Long memoryId, Model model) {
        ChatMemory source = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + memoryId));

        ChatMemory promoted = ChatMemory.builder()
                .chatChannelId(null)
                .key(source.getKey())
                .value(source.getValue())
                .build();
        memoryRepository.save(promoted);

        model.addAttribute("memory", promoted);
        return "admin/_memory_item_global";
    }

    // ── Participants ──

    @GetMapping("/participants/{id}")
    @ResponseBody
    public String participantsHtml(@PathVariable String id, Model model) {
        if ("null".equals(id)) {
            List<ChatParticipant> participants = participantRepository.findLastSeen();
            model.addAttribute("participants", participants);
            model.addAttribute("title", "All Participants");
            return "admin/_participants_list";
        } else {
            try {
                Long channelId = Long.parseLong(id);
                List<String> senders = messageRepository.findSenderNamesByChannel(channelId);
                model.addAttribute("senders", senders);
                model.addAttribute("title", "Channel Participants");
                return "admin/_channel_participants_list";
            } catch (NumberFormatException e) {
                return "<div class=\"empty-state\">Invalid channel ID.</div>";
            }
        }
    }

    private String formatRelative(Instant instant) {
        Duration duration = Duration.between(instant, Instant.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 30) return days + "d ago";
        long months = days / 30;
        return months + "mo ago";
    }
}
