package org.huebert.ncbot.service;

import lombok.RequiredArgsConstructor;
import org.huebert.ncbot.entity.ChatParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final org.huebert.ncbot.repository.ChatMessageRepository messageRepository;
    private final org.huebert.ncbot.repository.ChatParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public Page<ChatParticipant> findParticipantsByChannel(Long channelId, Pageable pageable) {
        Set<String> senders = messageRepository.findSenderNamesByChannel(channelId);
        return participantRepository.findParticipants(senders, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChatParticipant> findAllParticipants(Pageable pageable) {
        return participantRepository.findLastSeen(pageable);
    }
}
