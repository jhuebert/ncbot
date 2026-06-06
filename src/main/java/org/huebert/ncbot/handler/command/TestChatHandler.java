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
public class TestChatHandler implements CommandHandler {

    private static final Pattern PATTERN = Pattern.compile("^(t|test)$", Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public Map<String, Object> handle(ChatRequest request, Map<String, String> groups) {
        return Map.of(
                "template", "command/test"
        );
    }

}
