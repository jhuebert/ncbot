package org.huebert.ncbot.handler.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.handler.CommandChatHandler;
import org.huebert.ncbot.service.TemplateService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class TestChatHandler implements CommandChatHandler {

    private static final Set<String> COMMANDS = Set.of("t", "test");

    private final NcbotProperties ncbotProperties;

    private final TemplateService templateService;

    @Override
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {
        log.debug("handle: command={}", matches(request, COMMANDS));

        boolean command = ncbotProperties.getChannelProperties(request)
                .map(NcbotProperties.ChannelProperties::command)
                .orElse(false);
        if (!command || !matches(request, COMMANDS)) {
            return Optional.empty();
        }

        String response = templateService.render("command/test", Map.of(
                "request", request
        ));
        log.info("{} command from {} in {}", request.messageText(), request.senderName(), request.channelName());
        return Optional.of(response);
    }
}
