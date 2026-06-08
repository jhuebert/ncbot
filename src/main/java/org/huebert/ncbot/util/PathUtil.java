package org.huebert.ncbot.util;

import org.huebert.ncbot.dto.ChatRequest;

public class PathUtil {

    /**
     * Returns {@code true} if the request is using a 1-byte path (short path).
     */
    public static boolean isUsingOneBytePath(ChatRequest request) {
        Integer pathBytesPerHop = request.pathBytesPerHop();
        return pathBytesPerHop != null && pathBytesPerHop == 1;
    }

}
