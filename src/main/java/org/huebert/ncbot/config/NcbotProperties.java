package org.huebert.ncbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
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
        List<ChannelProperties> channels,
        Set<String> allowedDms,
        String welcomeContent,
        boolean condense
) {

    public record ChannelProperties(
            String name,
            boolean ai,
            boolean welcome,
            boolean command
    ) {
    }

}
