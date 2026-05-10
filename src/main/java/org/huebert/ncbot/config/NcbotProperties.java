package org.huebert.ncbot.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
