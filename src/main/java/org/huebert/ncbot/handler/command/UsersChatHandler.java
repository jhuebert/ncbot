package org.huebert.ncbot.handler.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.util.Truncate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Component
public class UsersChatHandler implements CommandHandler {

    private static final Pattern PATTERN = Pattern.compile("^(u|user|users)$", Pattern.CASE_INSENSITIVE);

    private final NcbotProperties ncbotProperties;

    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public Map<String, Object> handle(ChatRequest request, Map<String, String> groups) {

        List<String> users = chatParticipantRepository.findLastSeen().stream()
                .map(ChatParticipant::getName)
                .toList();

        String text = Truncate.joinWithLimit(users, ncbotProperties.maxReplyBytes(), "\n");

        return Map.of(
                "template", "command/users",
                "users", text.trim()
        );
    }

}
