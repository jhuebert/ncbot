package org.huebert.ncbot.tool;

import com.google.common.base.Utf8;
import lombok.RequiredArgsConstructor;
import org.huebert.ncbot.service.ConfigService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Lets the model measure the exact UTF-8 byte length of a candidate reply so it can
 * verify it fits the hard protocol limit before finalizing. LLMs are unreliable at
 * counting bytes (especially with multi-byte characters), so this provides ground truth.
 * The reply path never trusts the model on this: replies are always re-checked and
 * deterministically truncated by {@link org.huebert.ncbot.util.Truncate} as a fallback.
 */
@Component
@RequiredArgsConstructor
public class ByteLengthTool {

    private final ConfigService configService;

    @Tool(name = "checkBytes", description = "Return the exact UTF-8 byte length of a candidate reply and how far over the "
            + "hard protocol limit it is. Use this to make sure your FINAL reply fits: draft the full reply, "
            + "call this with the draft, and if it is over, trim words and re-check until it fits. "
            + "Plain ASCII (letters, digits, spaces, punctuation like @ [ ] $ % . , : /) is 1 byte per "
            + "character; emoji and non-ASCII symbols (Degree Sign, Euro Sign, accented letters, em-en dashes) "
            + "are 3-4 bytes each. The returned number is the source of truth — do not guess byte counts.")
    public ByteLengthResult checkBytes(
            @ToolParam(description = "The candidate reply text to measure — your full drafted response.") String text
    ) {
        int bytes = Utf8.encodedLength(text == null ? "" : text);
        int maxBytes = configService.maxReplyBytes();
        return new ByteLengthResult(bytes, maxBytes, Math.max(0, bytes - maxBytes));
    }

    public record ByteLengthResult(int bytes, int maxBytes, int overBy) {
        public boolean fits() {
            return overBy == 0;
        }
    }
}
