package org.huebert.ncbot.controller;

import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ChatMessageRepository messageRepository;
    private final NcbotProperties properties;
    private final Instant startTime;

    public AdminController(ChatMessageRepository messageRepository, NcbotProperties properties) {
        this.messageRepository = messageRepository;
        this.properties = properties;
        this.startTime = Instant.now();
    }

    @GetMapping
    public String dashboard(Model model) {
//        long totalMessages = messageRepository.count();
//        long channelMessages = messageRepository.findAll()
//                .stream().filter(m -> Boolean.FALSE.equals(m.getIsDm())).count();
//        long dmMessages = totalMessages - channelMessages;
//
//        Map<String, Long> channelStats = new LinkedHashMap<>();
//        for (String channelKey : messageRepository.findDistinctChannelKeys()) {
//            channelStats.put(channelKey, messageRepository.countByChannelKey(channelKey));
//        }
//
//        Duration uptime = Duration.between(startTime, Instant.now());
//        model.addAttribute("totalMessages", totalMessages);
//        model.addAttribute("channelMessages", channelMessages);
//        model.addAttribute("dmMessages", dmMessages);
//        model.addAttribute("channelStats", channelStats);
//        model.addAttribute("uptimeStr", formatUptime(uptime));
//        model.addAttribute("properties", properties);
        return "admin/index";
    }

    private String formatUptime(Duration d) {
        long days = d.toDaysPart();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m " + d.toSecondsPart() + "s";
    }

    @GetMapping("/messages")
    public String messages(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "25") int size) {
        List<ChatMessage> messages = messageRepository.findAll(
                PageRequest.of(page, size)).getContent();
        long totalPages = messageRepository.count() / size + 1;

        model.addAttribute("messages", messages);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("size", size);
        return "admin/messages";
    }

    @GetMapping("/config")
    public String config(Model model) {
        model.addAttribute("properties", properties);
        return "admin/config";
    }
}
