package org.huebert.ncbot.config;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.ChatRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;

@Slf4j
@ConfigurationProperties(prefix = "ncbot")
public record NcbotProperties(
        String systemPrompt,
        String condensePrompt,
        String memoryPrompt,
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
        String welcomeContent,
        boolean condense,
        int pathUpgradeCooldownMinutes,
        String name,
        List<String> blockUserPatterns,
        List<String> allowUserPatterns,
        List<String> blockPathPatterns,
        List<String> allowPathPatterns
) {

    private static final ChannelCapabilities DM_CAPABILITIES = new ChannelCapabilities(true, false, true, AiMode.EACH);

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
        return Optional.of(ChannelCapabilities.from(request.channelName(), this));
    }

    /**
     * Check whether command handling is enabled for the given request.
     */
    public boolean isCommandEnabled(ChatRequest request) {
        return getChannelCapabilities(request)
                .map(ChannelCapabilities::command)
                .orElse(false);
    }

}
