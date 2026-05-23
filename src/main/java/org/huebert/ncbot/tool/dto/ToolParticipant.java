package org.huebert.ncbot.tool.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ToolParticipant(
        String name,
        Instant lastSeen
) {
}
