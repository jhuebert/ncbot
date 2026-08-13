package org.huebert.ncbot.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.entity.ChatParticipant;
import org.huebert.ncbot.service.ParticipantService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lets the model answer "who is in / active in this channel". The recent-message block only
 * names the most recent senders, and the {@code users} command never reaches the AI, so an
 * on-demand participant query is needed for natural-language questions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantTool {

    private final ParticipantService participantService;

    @DebugLog
    @Tool(name = "getChannelParticipants", description = """
            List the people who have been seen in a channel, most recently active first, each with
            their 'name' and 'lastSeen' (ISO-8601 last time they sent a message). Use this when the
            user asks who is in or active in a channel — e.g. "who's online?", "is anyone here?",
            "who has been active today?". Judges recency from 'lastSeen'; note this is presence, not
            a live online state. The returned list is bounded by 'limit'.
            """)
    public List<ParticipantInfo> getChannelParticipants(
            @ToolParam(description = "Numeric id of the channel to query, from the CHANNEL_ID line in the prompt.") Long channelId,
            @ToolParam(description = "Maximum number of participants to return (default 50).") Integer limit
    ) {
        List<ChatParticipant> participants =
                participantService.findChannelParticipants(channelId, limit == null ? 50 : limit);
        return participants.stream()
                .map(p -> new ParticipantInfo(p.getName(),
                        p.getLastSeen() == null ? null : p.getLastSeen().toString()))
                .toList();
    }

    public record ParticipantInfo(String name, String lastSeen) {
    }

}
