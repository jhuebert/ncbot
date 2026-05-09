package org.huebert.ncbot.tool;

import java.nio.charset.StandardCharsets;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ByteCounterTool {

    @Tool(description = "Count the UTF-8 encoded byte length of a text string")
    public int countBytes(@ToolParam(description = "The text string to measure") String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
