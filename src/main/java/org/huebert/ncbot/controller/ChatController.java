package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.Delay;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.ChatResponse;
import org.huebert.ncbot.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
public class ChatController {

    private final ChatService chatService;

    private final NcbotProperties ncbotProperties;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        long start = System.currentTimeMillis();

        log.info("request: {}", request);
        ChatResponse response = chatService.processMessage(request);
        log.info("response: {}", response);

        long delay = ncbotProperties.minimumResponseMs() - (System.currentTimeMillis() - start);
        if (delay > 0) {
            Delay.sleep(delay);
        }

        return ResponseEntity.ok(response);
    }
}
