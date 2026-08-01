package org.huebert.ncbot.util;

import java.time.Duration;
import java.time.Instant;

/**
 * Compact human-readable age for prompt rendering, e.g. "just now", "5m ago",
 * "2h ago", "3d ago".
 *
 * Used in the chat prompt (__CHAT_MESSAGES__) so the model can judge whether
 * time-sensitive claims (weather, status, prices) are stale without doing
 * timestamp arithmetic. Kept deliberately coarse to minimize tokens.
 */
public final class RelativeTime {

    private RelativeTime() {
    }

    public static String format(Instant instant) {
        long seconds = Duration.between(instant, Instant.now()).getSeconds();
        if (seconds < 60) {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 48) {
            return hours + "h ago";
        }
        long days = hours / 24;
        return days + "d ago";
    }

}
