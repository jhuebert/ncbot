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
    public String getWeather(@ToolParam(description = "City name, coordinates, or zip code") String location) {
        log.info("getWeather: {}", location);
        String result = weatherService.getWeather(location)
                .orElse("Could not find weather for: " + location);
        log.info("getWeather result: {}", result);
        return result;
    }
}
