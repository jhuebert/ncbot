package org.huebert.ncbot.config;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ConfigurationProperties(prefix = "ncbot")
public record NcbotProperties(

        String name,

        String systemPrompt,
        String condensePrompt,
        String memoryPrompt,
        String welcomeContent,

        boolean condense,
        boolean allowOneBytePaths,
        int pathUpgradeCooldownMinutes,
        int memoryPartitionSize,
        long minimumResponseMs,
        int maxReplyBytes,

        List<String> channelsWelcome,
        List<String> channelsCommand,
        List<String> channelsPathUpgrade,
        List<String> channelsAiEach,
        List<String> channelsAiTagged,
        List<String> channelsAiDisabled,

        List<String> allowedDms,

        String blockUserPattern,
        String allowUserPattern,
        String blockPathPattern,
        String allowPathPattern
) {

    private static final ChannelCapabilities DM_CAPABILITIES = ChannelCapabilities.builder()
            .welcome(false)
            .pathUpgrade(false)
            .command(true)
            .ai(AiMode.EACH)
            .build();

    private static final Map<String, ChannelCapabilities> CHANNEL_CAPABILITIES = new ConcurrentHashMap<>();

    /**
     * Resolve channel capabilities for a given request.
     * DMs bypass channel lists and use default capabilities if the sender key is allowed.
     * Non-DMs resolve from flat property lists.
     */
    public Optional<ChannelCapabilities> getChannelCapabilities(ChatRequest request) {
        if (request.isDm()) {
            log.debug("getChannelCapabilities: DM from {}", request.senderKey());
            if (!allowedDms().isEmpty() && !allowedDms().contains(request.senderKey())) {
                log.debug("getChannelCapabilities: DM sender key {} not in allowed list", request.senderKey());
                return Optional.empty();
            }
            return Optional.of(DM_CAPABILITIES);
        }
        return Optional.of(getChannelCapabilities(request.channelName()));
    }

    public ChannelCapabilities getChannelCapabilities(String channelName) {
        return CHANNEL_CAPABILITIES.computeIfAbsent(channelName, this::from);
    }

    /**
     * Check whether command handling is enabled for the given request.
     */
    public boolean isCommandEnabled(ChatRequest request) {
        return getChannelCapabilities(request)
                .map(ChannelCapabilities::command)
                .orElse(false);
    }

    private ChannelCapabilities from(String channelName) {
        ChannelCapabilities result = ChannelCapabilities.builder()
                .welcome(channelsWelcome.contains(channelName))
                .command(channelsCommand.contains(channelName))
                .pathUpgrade(channelsPathUpgrade.contains(channelName))
                .ai(resolveAiMode(channelName))
                .build();
        log.debug("channel capabilities for {}: {}", channelName, result);
        return result;
    }

    private AiMode resolveAiMode(String channelName) {
        if (channelsAiDisabled.contains(channelName)) {
            return AiMode.DISABLED;
        }
        if (channelsAiEach.contains(channelName)) {
            return AiMode.EACH;
        }
        if (channelsAiTagged.contains(channelName)) {
            return AiMode.TAGGED;
        }
        return AiMode.DISABLED;
    }

}
