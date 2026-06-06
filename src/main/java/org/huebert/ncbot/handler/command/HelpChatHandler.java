package org.huebert.ncbot.handler.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Component
public class HelpChatHandler implements CommandHandler {

    private static final Pattern PATTERN = Pattern.compile("^(h|help)$", Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public Map<String, Object> handle(ChatRequest request, Map<String, String> groups) {
        return Map.of(
                "template", "command/help"
        );
    }

}
