package org.huebert.ncbot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherToolResponse {
    private final Integer temperatureFahrenheit;
    private final Integer windSpeedMilesPerHour;
    private final Integer windDirectionDegrees;
    private final Integer humidityPercent;
    private final String conditionsDescription;
}
