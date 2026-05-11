package org.huebert.ncbot.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CurrentTimeTool {

    @Tool(description = "Gets the current time")
    public String currentTime() {
        String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        log.debug("currentTime: {}", now);
        return now;
    }

}
