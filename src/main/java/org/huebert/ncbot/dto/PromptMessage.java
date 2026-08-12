package org.huebert.ncbot.dto;

import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.util.RelativeTime;

import java.time.Instant;

/**
 * A single chat message rendered into an AI prompt as JSON.
 * <p>
 * Only the fields the model needs are exposed (sender, text, timing, and the bot's
 * optional reply). DB ids, packet hashes, region, scoped flags, etc. are deliberately
 * omitted so they never consume context tokens.
 *
 * @param sender    display name of the message author
 * @param message   the message text (may contain newlines)
 * @param timestamp absolute ISO-8601 timestamp of the message
 * @param age       coarse human-readable age, e.g. "2h ago" (see {@link RelativeTime})
 * @param response  the bot's reply to this message, or {@code null} if there is none
 */
public record PromptMessage(
        String sender,
        String message,
        String timestamp,
        String age,
        String response
) {

    public static PromptMessage from(ChatMessage m) {
        Instant createdAt = m.getCreatedAt();
        return new PromptMessage(
                m.getSenderName(),
                m.getContent(),
                createdAt == null ? null : createdAt.toString(),
                createdAt == null ? null : RelativeTime.format(createdAt),
                m.getResponse()
        );
    }

    /**
     * Builds a prompt entry for the message currently being processed.
     */
    public static PromptMessage now(String sender, String message) {
        Instant now = Instant.now();
        return new PromptMessage(sender, message, now.toString(), RelativeTime.format(now), null);
    }

}