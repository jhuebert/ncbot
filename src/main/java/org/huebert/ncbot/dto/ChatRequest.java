package org.huebert.ncbot.dto;

import com.google.common.base.Splitter;
import org.apache.logging.log4j.util.Strings;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public record ChatRequest(
        String senderName,
        String senderKey,
        String messageText,
        boolean isDm,
        String channelKey,
        String channelName,
        Long senderTimestamp,
        String path,
        Boolean isOutgoing,
        Integer pathBytesPerHop,
        String packetHash,
        String region,
        Boolean scoped
) {

    public List<String> getPathItems() {
        String trimmed = Strings.trimToNull(path);
        if (trimmed == null) {
            return List.of("direct");
        }
        return Splitter.fixedLength(pathBytesPerHop * 2).splitToList(trimmed);
    }

    /**
     * Estimated mesh transit time from sender to ncbot, excluding RemoteTerm's
     * 2-second settle delay. Returns a human-readable string like "350ms", "3.2s",
     * or "unknown" if senderTimestamp is missing.
     */
    public String getTransitDescription() {
        return getTransitDuration(2_000)
                .map(d -> {
                    long ms = d.toMillis();
                    if (ms < 1000) {
                        return ms + "ms";
                    } else if (ms < 60_000) {
                        return String.format("%.1fs", ms / 1000.0);
                    } else {
                        long minutes = ms / 60_000;
                        long seconds = (ms % 60_000) / 1000;
                        return minutes + "m " + seconds + "s";
                    }
                })
                .orElse("unknown");
    }

    /**
     * Transit time as an {@link Optional} {@link Duration} with a configurable
     * bot settle delay, or {@link Optional#empty()} if senderTimestamp is missing.
     *
     * @param botDelayMs the RemoteTerm settle delay in milliseconds (default 2000)
     */
    public Optional<Duration> getTransitDuration(long botDelayMs) {
        if (senderTimestamp == null) {
            return Optional.empty();
        }
        long nowMs = System.currentTimeMillis();
        long senderMs = senderTimestamp * 1000;
        return Optional.of(Duration.ofMillis(Math.max(0, nowMs - senderMs - botDelayMs)));
    }

}
