package org.huebert.ncbot.tool;

import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CountBytesTool {

    @Tool(description = "Get the UTF-8 encoded byte length of a text string")
    public int countBytes(@ToolParam(description = "The text string to measure") String text) {
        log.info("countBytes text: {}", text);
        int length = text.getBytes(StandardCharsets.UTF_8).length;
        log.info("countBytes result: {}", length);
        return length;
    }

}
