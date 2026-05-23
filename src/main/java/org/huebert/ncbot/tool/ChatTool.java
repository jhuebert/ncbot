package org.huebert.ncbot.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.huebert.ncbot.tool.dto.ToolParticipant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatTool {

    private final ChatChannelRepository chatChannelRepository;

    private final ChatParticipantRepository chatParticipantRepository;

    @Tool(description = "Get known Meshcore channels")
    public List<String> getKnownChannels() {
        log.debug("getKnownChannels");
        List<String> channels = chatChannelRepository.findPublicChannels();
        log.debug("getKnownChannels result: {}", channels);
        return channels;
    }

//    @Tool(description = "Get information about a Meshcore users")
//    public List<ToolParticipant> findUsers(String search) {
//        log.debug("findUsers: {}", search);
//        List<ToolParticipant> participants = chatParticipantRepository.searchParticipants('%' + search + '%').stream()
//                .map(p -> {
//                    return ToolParticipant.builder()
//                            .name(p.getName())
//                            .lastSeen(p.getLastSeen())
//                            .build();
//                })
//                .toList();
//        log.debug("findUsers result: {}", participants);
//        return participants;
//    }

}
