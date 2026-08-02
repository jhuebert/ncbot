package org.huebert.ncbot.controller.dto;

import lombok.Builder;

@Builder
public record ParticipantUpdateRequest(boolean blocked) {
}
