package org.huebert.ncbot.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.repository.ChatChannelRepository;
import org.huebert.ncbot.repository.ChatParticipantRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    @Tool(description = "Searches Meshcore users")
    public List<String> searchUsers(
            @ToolParam(description = "Search string that returned user names must contain. Use empty or null to get all users.") String search
    ) {
        log.debug("findUsers: {}", search);
        List<ChatParticipant> chatParticipants;
        if (Strings.isNotBlank(search)) {
            chatParticipants = chatParticipantRepository.searchParticipants('%' + search.trim() + '%');
        } else {
            chatParticipants = chatParticipantRepository.findAll();
        }
        List<String> participants = chatParticipants.stream()
                .map(ChatParticipant::getName)
                .toList();
        log.debug("findUsers result: {}", participants);
        return participants;
    }

}
