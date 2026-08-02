package org.huebert.ncbot;

import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.handler.BlockingChatHandler;
import org.huebert.ncbot.handler.ChatHandler;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.service.ParticipantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ParticipantBlockingTest {

    @Autowired
    private ChatParticipantRepository participantRepository;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private BlockingChatHandler blockingChatHandler;

    private ChatRequest requestFrom(String senderName) {
        return new ChatRequest(
                senderName,
                "key",
                "hello",
                false,
                "channelKey",
                "#test",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void unknownParticipantIsNotBlocked() {
        ChatRequest request = requestFrom("stranger-" + System.nanoTime());
        assertEquals(Optional.empty(), blockingChatHandler.handle(null, request));
    }

    @Test
    void blockedParticipantShortCircuitsHandler() {
        Instant now = Instant.now();
        ChatParticipant participant = participantRepository.save(ChatParticipant.builder()
                .name("spammer-" + System.nanoTime())
                .firstSeen(now)
                .lastSeen(now)
                .build());

        ChatRequest request = requestFrom(participant.getName());
        assertEquals(Optional.empty(), blockingChatHandler.handle(null, request));

        participantRepository.save(participant.toBuilder().blocked(true).build());
        assertEquals(Optional.of(ChatHandler.DO_NOT_RESPOND), blockingChatHandler.handle(null, request));

        participantService.updateBlocked(participant.getId(), false);
        assertEquals(Optional.empty(), blockingChatHandler.handle(null, request));
    }

    @Test
    void updateBlockedThrowsForUnknownParticipant() {
        assertThrows(IllegalArgumentException.class, () -> participantService.updateBlocked(Long.MAX_VALUE, true));
    }

}
