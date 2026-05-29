package org.huebert.ncbot.dto;

import com.google.common.base.Splitter;
import org.apache.logging.log4j.util.Strings;

import java.util.List;

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
        Integer pathBytesPerHop
) {

    public List<String> getPathItems() {
        String trimmed = Strings.trimToNull(path);
        if (trimmed == null) {
            return List.of("direct");
        }
        return Splitter.fixedLength(pathBytesPerHop * 2).splitToList(trimmed);
    }

}
