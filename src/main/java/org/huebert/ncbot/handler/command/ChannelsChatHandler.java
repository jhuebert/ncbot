package org.huebert.ncbot.handler.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.handler.CommandChatHandler;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.service.TemplateService;
import org.huebert.ncbot.util.Truncate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChannelsChatHandler implements CommandChatHandler {

    private static final Set<String> COMMANDS = Set.of("c", "channel", "channels");

    private final NcbotProperties ncbotProperties;

    private final TemplateService templateService;

    private final ChatChannelRepository chatChannelRepository;

    private final ChatMessageRepository chatMessageRepository;

    @Override
    public Optional<String> handle(ChatChannel chatChannel, ChatRequest request) {
        log.debug("handle: command={}", matches(request, COMMANDS));

        if (!ncbotProperties.isCommandEnabled(request) || !matches(request, COMMANDS)) {
            return Optional.empty();
        }

        Map<Long, ChatChannel> channels = chatChannelRepository.findPublicChannels().stream()
                .collect(Collectors.toMap(ChatChannel::getId, a -> a));

        List<String> channelNames = chatMessageRepository.findLastSeen(channels.keySet()).stream()
                .map(a -> channels.get(a.key()).getChannelName())
                .toList();

        String text = Truncate.joinWithLimit(channelNames, ncbotProperties.maxReplyBytes(), " ");

        String response = templateService.render("command/channels", Map.of(
                "request", request,
                "channels", text.trim()
        ));
        log.info("{} command from {} in {} ({} channels)", request.messageText(), request.senderName(), request.channelName(), channelNames.size());
        return Optional.of(response);
    }

}
