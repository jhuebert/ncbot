package org.huebert.ncbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "ncbot")
public record NcbotProperties(
        String systemPrompt,
        String condensePrompt,
        String memoryPrompt,
        int memoryUpdateMinutes,
        int memoryPartitionSize,
        long minimumResponseMs,
        int maxReplyBytes,
        Set<String> commands,
        Set<String> allowedChannels,
        Set<String> allowedDms
) {
}
