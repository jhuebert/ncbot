package org.huebert.ncbot.controller;

import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
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
        ChatChannel publicChannel = publicChannels.stream()
                .filter(c -> "Public".equals(c.getChannelName()))
                .findFirst()
                .orElse(null);

        List<ChatMessage> messages = List.of();
        Long channelId = null;
        String channelName = "No channels";

        if (publicChannel != null) {
            messages = messageRepository.findMessagesByChannelOrderByCreatedDesc(
                    publicChannel.getId(), PAGE_SIZE);
            channelId = publicChannel.getId();
            channelName = publicChannel.getChannelName();
        }

        // Pre-render messages HTML for initial page load
        String fmt = formatRelative(Instant.now());
        Map<String, Object> msgParams = new HashMap<>();
        msgParams.put("messages", messages);
        msgParams.put("channelId", channelId);
        msgParams.put("formatRelative", fmt);
        String messagesHtml = renderTemplate("admin/_messages_list", msgParams);

        model.addAttribute("publicChannels", publicChannels);
        model.addAttribute("dmChannels", dmChannels);
        model.addAttribute("messagesHtml", messagesHtml);
        model.addAttribute("channelId", channelId);
        model.addAttribute("channelName", channelName);

        return "admin/index";
    }

    // ── Messages ──

    @GetMapping("/messages/{id}")
    @ResponseBody
    public String messagesHtml(@PathVariable Long id) {
        List<ChatMessage> messages = messageRepository.findMessagesByChannelOrderByCreatedDesc(id, PAGE_SIZE);
        String fmt = formatRelative(Instant.now());
        Map<String, Object> params = new HashMap<>();
        params.put("messages", messages);
        params.put("channelId", id);
        params.put("formatRelative", fmt);
        return renderTemplate("admin/_messages_list", params);
    }

    @GetMapping("/messages/{id}/older")
    @ResponseBody
    public String olderMessages(@PathVariable Long id,
                                @RequestParam(required = false) String before) {
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
        Map<String, Object> params = new HashMap<>();
        params.put("messages", messages);
        params.put("formatRelative", fmt);
        return renderTemplate("admin/_messages_list_older", params);
    }

    // ── Memory ──

    @GetMapping("/memory/{id}")
    @ResponseBody
    public String memoryHtml(@PathVariable Long id) {
        List<ChatMemory> memories = memoryRepository.findMemory(id);
        Map<String, Object> params = new HashMap<>();
        params.put("memories", memories);
        params.put("title", "Channel Memory");
        params.put("channelId", id);
        params.put("listId", "memory-list-" + id);
        return renderTemplate("admin/_memory_list", params);
    }

    @GetMapping("/memory/global")
    @ResponseBody
    public String globalMemoryHtml() {
        List<ChatMemory> memories = memoryRepository.findGlobalMemory();
        Map<String, Object> params = new HashMap<>();
        params.put("memories", memories);
        params.put("title", "Global Memory");
        params.put("channelId", null);
        params.put("listId", "global-memory-list");
        return renderTemplate("admin/_memory_list", params);
    }

    @PostMapping("/memory")
    @ResponseBody
    public String addMemory(@RequestParam Long channelId,
                            @RequestParam String key,
                            @RequestParam String value) {
        ChatMemory memory = ChatMemory.builder()
                .chatChannelId(channelId)
                .key(key)
                .value(value)
                .build();
        memoryRepository.save(memory);
        Map<String, Object> params = new HashMap<>();
        params.put("memory", memory);
        params.put("channelId", channelId);
        return renderTemplate("admin/_memory_item", params);
    }

    @PutMapping("/memory/{id}")
    @ResponseBody
    public String updateMemory(@PathVariable Long id,
                               @RequestParam String key,
                               @RequestParam String value) {
        ChatMemory memory = memoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + id));
        memory.setKey(key);
        memory.setValue(value);
        memoryRepository.save(memory);

        Long channelId = memory.getChatChannelId();
        Map<String, Object> params = new HashMap<>();
        params.put("memory", memory);
        params.put("channelId", channelId);
        return renderTemplate("admin/_memory_item", params);
    }

    @DeleteMapping("/memory/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteMemory(@PathVariable Long id) {
        memoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/memory/promote")
    @ResponseBody
    public String promoteMemory(@RequestParam Long memoryId) {
        ChatMemory source = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + memoryId));

        ChatMemory promoted = ChatMemory.builder()
                .chatChannelId(null)
                .key(source.getKey())
                .value(source.getValue())
                .build();
        memoryRepository.save(promoted);

        Map<String, Object> params = new HashMap<>();
        params.put("memory", promoted);
        return renderTemplate("admin/_memory_item_global", params);
    }

    // ── Participants ──

    @GetMapping("/participants/{id}")
    @ResponseBody
    public String participantsHtml(@PathVariable String id) {
        if ("null".equals(id)) {
            List<ChatParticipant> participants = participantRepository.findLastSeen();
            Map<String, Object> params = new HashMap<>();
            params.put("participants", participants);
            params.put("title", "All Participants");
            return renderTemplate("admin/_participants_list", params);
        } else {
            try {
                Long channelId = Long.parseLong(id);
                List<String> senders = messageRepository.findSenderNamesByChannel(channelId);
                Map<String, Object> params = new HashMap<>();
                params.put("senders", senders);
                params.put("title", "Channel Participants");
                return renderTemplate("admin/_channel_participants_list", params);
            } catch (NumberFormatException e) {
                return "<div class=\"empty-state\">Invalid channel ID.</div>";
            }
        }
    }

    // ── Helpers ──

    private String renderTemplate(String name, Map<String, Object> params) {
        TemplateOutput output = new StringOutput();
        templateEngine.render(name, params, output);
        return output.toString();
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
