package org.huebert.ncbot.config;

import lombok.Builder;

/**
 * Resolved channel capabilities derived from flat configuration lists.
 */
@Builder
public record ChannelCapabilities(
        boolean welcome,
        boolean pathUpgrade,
        boolean command,
        AiMode ai
) {
}
