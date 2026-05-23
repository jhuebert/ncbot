package org.huebert.ncbot.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Builder;

@Builder
public record WeatherToolResponse(
        @JsonPropertyDescription("Current temperature in Fahrenheit") Integer temperature,
        @JsonPropertyDescription("Current wind speed in miles per hour") Integer windSpeed,
        @JsonPropertyDescription("Current wind direction azimuth in degrees") Integer windDirection,
        @JsonPropertyDescription("Current humidity percent. Value from 0-100") Integer humidity,
        @JsonPropertyDescription("Current conditions description") String conditions
) {
}
