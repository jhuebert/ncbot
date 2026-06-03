package org.huebert.ncbot.controller.dto;

import java.time.Instant;

public record ParticipantDto(String name, Instant lastSeen) {
}
