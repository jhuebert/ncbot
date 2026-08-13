package org.huebert.ncbot.service;

import lombok.RequiredArgsConstructor;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public Page<ChatParticipant> findParticipantsByChannel(Long channelId, String query, Pageable pageable) {
        Set<String> senders = messageRepository.findSenderNamesByChannel(channelId);
        String q = normalized(query);
        if (q != null) {
            String lower = q.toLowerCase();
            senders = senders.stream()
                    .filter(name -> name.toLowerCase().contains(lower))
                    .collect(Collectors.toSet());
        }
        return participantRepository.findParticipants(senders, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChatParticipant> findAllParticipants(String query, Pageable pageable) {
        String q = normalized(query);
        return q == null
                ? participantRepository.findLastSeen(pageable)
                : participantRepository.searchLastSeen(q, pageable);
    }

    /**
     * Sets the explicit block state of a participant. Returns the updated
     * participant, or throws {@link IllegalArgumentException} if not found.
     */
    @Transactional
    public ChatParticipant updateBlocked(Long id, boolean blocked) {
        ChatParticipant participant = participantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + id));
        participant.setBlocked(blocked);
        return participantRepository.save(participant);
    }

    private static String normalized(String query) {
        return query == null ? null : query.trim();
    }

    /**
     * Participants seen in a single channel (derived from that channel's message senders),
     * most recently active first. Read path for the getChannelParticipants AI tool.
     */
    @Transactional(readOnly = true)
    public List<ChatParticipant> findChannelParticipants(Long channelId, int limit) {
        return findParticipantsByChannel(channelId, null, PageRequest.of(0, Math.max(1, limit)))
                .getContent();
    }

}