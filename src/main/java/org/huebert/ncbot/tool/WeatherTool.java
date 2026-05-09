package org.huebert.ncbot.tool;

import org.huebert.ncbot.service.WeatherService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {

    private final WeatherService weatherService;

    public WeatherTool(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Tool(description = "Get current weather conditions for a location")
    public String getWeather(@ToolParam(description = "City name, coordinates, or zip code") String location) {
        return weatherService.getWeather(location)
            .orElse("Could not find weather for: " + location);
    }
}
