package org.huebert.ncbot.config;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.huebert.ncbot.config.AiMode.*;

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
        int pathUpgradeCooldownMinutes,
        String name
) {

    private static final ChannelProperties DM_PROPERTIES = new ChannelProperties(null, false, false, true, EACH);

    public Optional<ChannelProperties> getChannelProperties(ChatRequest request) {
        if (request.isDm()) {
            log.debug("getChannelProperties: DM from {}, returning default properties", request.senderKey());
            return Optional.of(DM_PROPERTIES);
        }
        return channels().stream()
                .filter(a -> a.name().equals(request.channelName()))
                .findFirst()
                .or(() -> {
                    log.info("no channel config for '{}', available: {}", request.channelName(), channels().stream().map(ChannelProperties::name).toList());
                    return Optional.empty();
                });
    }

    public record ChannelProperties(
            String name,
            boolean welcome,
            boolean pathUpgrade,
            boolean command,
            AiMode ai
            ) {
    }

}
