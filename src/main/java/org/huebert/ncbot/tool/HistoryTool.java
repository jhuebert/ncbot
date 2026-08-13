package org.huebert.ncbot.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.PromptMessage;
import org.huebert.ncbot.service.MessageService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Lets the model query the raw chat history of a channel on demand — the rolling window
 * in {@code __CHAT_MESSAGES__} only covers the last ~30 min / recent messages, and long-term
 * memory deliberately discards ephemeral content, so summarization and "what did we discuss
 * about X" questions must read the {@code chat_message} table directly.
 * <p>
 * The returned JSON array uses the exact same shape as {@code __CHAT_MESSAGES__} (see
 * {@link PromptMessage}) so the model reasons over it with the same mental model.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryTool {

    private final MessageService messageService;

    @DebugLog
    @Tool(name = "getHistory", description = """
            Query the raw chat history of a channel. Use this ONLY when the user asks to summarize,
            recap, or dig into messages from before the recent conversation, or to find specific past
            messages — the recent-message block in the prompt only covers the last ~30 minutes, so
            you cannot answer history questions from it alone.
            Returns a JSON array of messages in the SAME format as the __CHAT_MESSAGES__ block (oldest
            first): each entry has "sender", "message", "timestamp", "age", and optional "response" (the
            bot's reply). Bounded by 'limit' (newest messages matching the filters).
            For an open-ended summary of a period, pass 'from'/'to' and omit 'term' so you get the full
            window. To find specific discussion, pass 'term' (case-insensitive substring of the message
            text) — note that then ONLY matching messages are returned, so you may need a second range
            call for surrounding context. Pass 'sender' to restrict to one person's messages.
            """)
    public List<PromptMessage> getHistory(
            @ToolParam(description = "Numeric id of the channel to query, from the CHANNEL_ID line in the prompt.") Long channelId,
            @ToolParam(description = "ISO-8601 start instant (inclusive); omit for no lower bound. e.g. 2024-01-01T00:00:00Z") String from,
            @ToolParam(description = "ISO-8601 end instant (exclusive); omit for no upper bound. e.g. 2024-01-01T23:59:59Z") String to,
            @ToolParam(description = "Exact sender name to keep only their messages; omit for everyone.") String sender,
            @ToolParam(description = "Case-insensitive substring that the message text must contain; omit for all messages.") String term,
            @ToolParam(description = "Maximum number of messages to return (default 50, capped at 500).") Integer limit
    ) {
        Integer n = limit == null ? 50 : limit;
        return messageService.searchHistory(
                channelId, parseInstant(from), parseInstant(to), sender, term, n);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

}
