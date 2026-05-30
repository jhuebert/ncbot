package org.huebert.ncbot.config;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@ConfigurationProperties(prefix = "ncbot")
public record NcbotProperties(
        String systemPrompt,
        String condensePrompt,
        String memoryPrompt,
        int memoryUpdateMinutes,
        int memoryPartitionSize,
        long minimumResponseMs,
        int maxReplyBytes,
        List<ChannelProperties> channels,
        Set<String> allowedDms,
        String welcomeContent,
        boolean condense,
        int pathUpgradeCooldownMinutes
) {

    private static final ChannelProperties DM_PROPERTIES = new ChannelProperties(null, true, false, true, false, false);

    public Optional<ChannelProperties> getChannelProperties(ChatRequest request) {
        if (request.isDm()) {
            return Optional.of(DM_PROPERTIES);
        }
        return channels().stream()
                .filter(a -> a.name().equals(request.channelName()))
                .findFirst();
    }

    public record ChannelProperties(
            String name,
            boolean ai,
            boolean welcome,
            boolean command,
            boolean pathUpgrade,
            boolean respondOnTag
    ) {
    }

}
