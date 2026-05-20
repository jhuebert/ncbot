package org.huebert.ncbot.dto;

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
}
