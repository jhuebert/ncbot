package org.huebert.ncbot.dto;

import java.util.List;

public record ChatResponse(
        List<String> replies
) {
}
