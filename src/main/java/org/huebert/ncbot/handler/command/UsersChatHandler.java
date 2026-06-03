package org.huebert.ncbot.handler.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.handler.CommandChatHandler;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.service.TemplateService;
import org.huebert.ncbot.util.Truncate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class UsersChatHandler implements CommandChatHandler {

    private static final Set<String> COMMANDS = Set.of("u", "user", "users");

    private final NcbotProperties ncbotProperties;

    private final TemplateService templateService;

    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {
        log.debug("handle: command={}", matches(request, COMMANDS));

        if (!ncbotProperties.isCommandEnabled(request) || !matches(request, COMMANDS)) {
            return Optional.empty();
        }

        List<String> users = chatParticipantRepository.findLastSeen().stream()
                .map(ChatParticipant::getName)
                .toList();

        String text = Truncate.joinWithLimit(users, ncbotProperties.maxReplyBytes(), "\n");

        String response = templateService.render("command/users", Map.of(
                "request", request,
                "users", text.trim()
        ));
        log.info("{} command from {} in {} ({} users)", request.messageText(), request.senderName(), request.channelName(), users.size());
        return Optional.of(response);
    }

}
