package org.huebert.ncbot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.handler.command.CommandHandler;
import org.huebert.ncbot.service.TemplateService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandChatHandler implements ChatHandler {

    private static final int ORDER = 50;

    private final NcbotProperties ncbotProperties;

    private final TemplateService templateService;

    private final List<CommandHandler> commandHandlers;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {

        if (!ncbotProperties.isCommandEnabled(request)) {
            return Optional.empty();
        }

        for (CommandHandler handler : commandHandlers) {
            Map<String, String> groups = getGroups(request, handler);
            if (groups == null) {
                continue;
            }


            Map<String, Object> handlerParams = handler.handle(request, groups);
            if (handlerParams == null) {
                continue;
            }

            Object template = handlerParams.get("template");
            if (template == null) {
                continue;
            }

            Map<String, Object> templateParams = new HashMap<>(handlerParams);
            templateParams.put("request", request);
            return Optional.of(templateService.render(template.toString(), templateParams));

        }
        return Optional.empty();
    }

    private Map<String, String> getGroups(ChatRequest request, CommandHandler handler) {
        String trimmed = Optional.ofNullable(request.messageText())
                .map(String::trim)
                .orElse("");
        Pattern pattern = handler.getPattern();
        Matcher matcher = pattern.matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        pattern.namedGroups().keySet()
                .forEach(name -> map.put(name, matcher.group(name)));
        return map;
    }

}
