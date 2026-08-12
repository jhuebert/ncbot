package org.huebert.ncbot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.huebert.ncbot.dto.PromptMessage;

import java.util.List;

/**
 * Renders {@link PromptMessage}s as a compact JSON array for AI prompts.
 * <p>
 * JSON is used instead of "sender: message" lines so that multi-line messages and
 * text that happens to resemble the old prefix format cannot be mis-parsed by the
 * model. The array is serialized compactly to keep token usage low.
 */
public final class PromptMessages {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PromptMessages() {
    }

    public static String toJson(List<PromptMessage> messages) {
        try {
            return MAPPER.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize prompt messages", e);
        }
    }

}