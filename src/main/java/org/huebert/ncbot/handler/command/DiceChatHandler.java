package org.huebert.ncbot.handler.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Component
public class DiceChatHandler implements CommandHandler {

    private static final Pattern PATTERN = Pattern.compile("^d(?<sides>[1-9]\\d*)$", Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public Map<String, Object> handle(ChatRequest request, Map<String, String> groups) {
        return Map.of(
                "template", "command/dice",
                "value", new Random().nextInt(1, Integer.parseInt(groups.get("sides")) + 1)
        );
    }

}
