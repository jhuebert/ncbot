package org.huebert.ncbot.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.service.WeatherService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTool {

    private final WeatherService weatherService;

    @Tool(description = "Get current weather conditions for a location")
    public String getWeather(@ToolParam(description = "Zip code") String zipCode) {
        log.debug("getWeather: {}", zipCode);
        String result = weatherService.getWeather(zipCode)
                .orElse("Could not find weather for: " + zipCode);
        log.debug("getWeather result: {}", result);
        return result;
    }
}
