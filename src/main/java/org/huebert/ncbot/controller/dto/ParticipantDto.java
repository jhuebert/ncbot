package org.huebert.ncbot.controller.dto;

import lombok.Builder;
import org.huebert.ncbot.entity.ChatParticipant;

import java.time.Instant;

@Builder
public record ParticipantDto(
        Long id,
        String name,
        Instant firstSeen,
        Instant lastSeen,
        Instant pathUpgradeNotifiedAt,
        boolean blocked
) {

    public static ParticipantDto from(ChatParticipant p) {
        return ParticipantDto.builder()
                .id(p.getId())
                .name(p.getName())
                .firstSeen(p.getFirstSeen())
                .lastSeen(p.getLastSeen())
                .pathUpgradeNotifiedAt(p.getPathUpgradeNotifiedAt())
                .blocked(p.isBlocked())
                .build();
    }

}
