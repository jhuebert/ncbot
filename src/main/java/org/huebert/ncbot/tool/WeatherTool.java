package org.huebert.ncbot.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.WeatherApiResponse;
import org.huebert.ncbot.dto.WeatherCode;
import org.huebert.ncbot.dto.WeatherToolResponse;
import org.huebert.ncbot.service.WeatherService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTool {

    private final WeatherService weatherService;

    @DebugLog
    @Tool(description = "Get current weather for a location. Temperature is in degrees "
            + "Fahrenheit, wind speed in mph, wind direction in degrees, humidity in percent. "
            + "Call only when the user asks about current weather and the data is not already "
            + "fresh in the conversation (a weather claim older than ~15 minutes is stale).")
    public WeatherToolResponse getCurrentWeather(
            @ToolParam(description = "Latitude in decimal degrees (e.g. 40.7128). Estimate from the "
                    + "location name if the exact value is unknown.") double latitude,
            @ToolParam(description = "Longitude in decimal degrees (e.g. -74.0060). Estimate from "
                    + "the location name if the exact value is unknown.") double longitude
    ) {
        return weatherService.getWeather(latitude, longitude)
                .map(r -> WeatherToolResponse.builder()
                        .conditions(WeatherCode.fromCode(r.current().weatherCode()).getDescription())
                        .temperature(getTemperature(r))
                        .humidity(r.current().relativeHumidity2m())
                        .windSpeed(getWindSpeed(r))
                        .windDirection(r.current().windDirection10m())
                        .build())
                .orElse(null);
    }

    private static int getTemperature(WeatherApiResponse response) {
        double temperature = response.current().temperature2m();
        if ("°C".equals(response.currentUnits().temperature2m())) {
            temperature = (1.8 * temperature) + 32.0;
        }
        return (int) Math.rint(temperature);
    }

    private static int getWindSpeed(WeatherApiResponse response) {
        double windSpeed = response.current().windSpeed10m();
        if ("km/h".equals(response.currentUnits().windSpeed10m())) {
            windSpeed = 0.6213711922 * windSpeed;
        }
        return (int) Math.rint(windSpeed);
    }

}
