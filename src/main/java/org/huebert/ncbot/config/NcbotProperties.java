package org.huebert.ncbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ncbot")
public record NcbotProperties(
        String systemPrompt,
        long minimumResponseMs,
        int messageHistoryMinutes,
        int maxReplyBytes,
        List<String> allowedChannels,
        boolean allowDms
) {
}
