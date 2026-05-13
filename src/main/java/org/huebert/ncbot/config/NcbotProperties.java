package org.huebert.ncbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ncbot")
public record NcbotProperties(
        String systemPrompt,
        String condensePrompt,
        String memoryPrompt,
        String combinePrompt,
        long minimumResponseMs,
        int maxReplyBytes,
        List<String> allowedChannels,
        boolean allowDms
) {
}
