package org.huebert.ncbot.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ncbot")
public record NcbotProperties(
    String systemPrompt,
    double responseDelaySeconds,
    int maxReplyBytes,
    int historyLimit,
    List<String> allowedChannels,
    boolean allowDms
) {
    public NcbotProperties {
        if (systemPrompt == null) {
            systemPrompt = """
                You are ncbot, an AI assistant on the Meshcore mesh network.
                You receive messages from chat channels and DMs.

                Guidelines:
                - Only respond when directly addressed or when you have something useful to add.
                - Do not respond to casual conversation between other users.
                - Keep responses concise — each message must be 128 UTF-8 bytes or fewer.
                - Prefer a single message response. Only use multiple messages when the content genuinely cannot fit.
                - If a response is too long, use the byte_counter tool to check and shorten it.
                - You can use tools to enhance your responses.
                - Common commands users may try:
                  * "test" or "t" — respond with connection info from the message details
                  * "ping" — respond with "pong"
                  * "path", "decode", or "route" — respond with the hex-encoded routing path from the message
                  * "help" — list available capabilities
                  * "hello"/"hi"/"hey" — friendly greeting
                - Understand intent — "tes", "tesy", "tset" likely mean "test"
                - Users may try commands from other MeshCore bots (wx, gwx, sun, moon,
                  solar, dice, joke, dadjoke, catfact, sports, status, version, etc.).
                  Only respond to commands you actually support. For unsupported commands,
                  politely decline or ignore.
                """;
        }
        if (responseDelaySeconds < 0) {
            responseDelaySeconds = 0;
        }
        if (maxReplyBytes <= 0) {
            maxReplyBytes = 128;
        }
        if (historyLimit <= 0) {
            historyLimit = 20;
        }
    }
}
