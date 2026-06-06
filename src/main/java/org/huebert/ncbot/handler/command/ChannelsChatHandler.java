package org.huebert.ncbot.handler.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatChannel;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.util.Truncate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChannelsChatHandler implements CommandHandler {

    private static final Pattern PATTERN = Pattern.compile("^(c|channel|channels)$", Pattern.CASE_INSENSITIVE);

    private final NcbotProperties ncbotProperties;

    private final ChatChannelRepository chatChannelRepository;

    private final ChatMessageRepository chatMessageRepository;

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public Map<String, Object> handle(ChatRequest request, Map<String, String> groups) {

        Map<Long, ChatChannel> channels = chatChannelRepository.findPublicChannels().stream()
                .collect(Collectors.toMap(ChatChannel::getId, a -> a));

        List<String> channelNames = chatMessageRepository.findLastSeen(channels.keySet()).stream()
                .map(a -> channels.get(a.key()).getChannelName())
                .toList();

        String text = Truncate.joinWithLimit(channelNames, ncbotProperties.maxReplyBytes(), " ");
        return Map.of(
                "template", "command/channels",
                "channels", text.trim()
        );
    }

}
