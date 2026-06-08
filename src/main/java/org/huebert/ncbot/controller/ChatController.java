package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.ChatResponse;
import org.huebert.ncbot.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        try {
            log.debug("request: {}", request);
            ChatResponse response = chatService.processMessage(request);
            if (!response.replies().isEmpty()) {
                String reply = response.replies().getFirst();
                log.debug("response: {} ({} bytes)", reply, reply.length());
                long delay = ncbotProperties.minimumResponseMs() - (System.currentTimeMillis() - start);
                if (delay > 0) {
                    log.debug("delaying {} ms", delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        log.error("interrupted", e);
                    }
                }
            } else {
                log.debug("no response generated");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing message from {} in {}: {}", request.senderName(), request.channelName(), e.getMessage(), e);
            return ResponseEntity.ok(new ChatResponse(List.of()));
        }
    }

}
