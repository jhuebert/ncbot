package org.huebert.ncbot.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.WeatherApiResponse;
import org.huebert.ncbot.dto.WeatherCode;
import org.huebert.ncbot.dto.WeatherToolResponse;
import org.huebert.ncbot.service.WeatherService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTool {

    private final WeatherService weatherService;

    @Tool(description = "Get current weather for a location")
    public WeatherToolResponse getCurrentWeather(
            @ToolParam(description = "Latitude") double latitude,
            @ToolParam(description = "Longitude") double longitude
    ) {
        log.debug("getCurrentWeather: latitude={}, longitude={}", latitude, longitude);
        WeatherToolResponse result = weatherService.getWeather(latitude, longitude)
                .map(r -> WeatherToolResponse.builder()
                        .conditionsDescription(WeatherCode.fromCode(r.current().weatherCode()).getDescription())
                        .temperatureFahrenheit(getTemperature(r))
                        .humidityPercent(r.current().relativeHumidity2m())
                        .windSpeedMilesPerHour(getWindSpeed(r))
                        .windDirectionDegrees(r.current().windDirection10m())
                        .build())
                .orElse(null);
        log.debug("getCurrentWeather result: {}", result);
        return result;
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
