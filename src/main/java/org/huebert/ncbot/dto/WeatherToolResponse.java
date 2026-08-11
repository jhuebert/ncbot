package org.huebert.ncbot.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Builder;

import java.util.List;

@Builder
public record WeatherToolResponse(
        @JsonPropertyDescription("Location latitude in decimal degrees") Double latitude,
        @JsonPropertyDescription("Location longitude in decimal degrees") Double longitude,
        @JsonPropertyDescription("Time zone of the location, e.g. America/New_York") String timezone,
        @JsonPropertyDescription("Current observed weather conditions") Current current,
        @JsonPropertyDescription("Daily weather forecast, one entry per day (7 days)") List<Day> forecast
) {

    @Builder
    public record Current(
            @JsonPropertyDescription("Current temperature in Fahrenheit") Integer temperature,
            @JsonPropertyDescription("Current apparent/feels-like temperature in Fahrenheit") Integer apparentTemperature,
            @JsonPropertyDescription("Current wind speed in miles per hour") Integer windSpeed,
            @JsonPropertyDescription("Current wind gust speed in miles per hour") Integer windGusts,
            @JsonPropertyDescription("Current wind direction azimuth in degrees") Integer windDirection,
            @JsonPropertyDescription("Current humidity percent. Value from 0-100") Integer humidity,
            @JsonPropertyDescription("Current precipitation in inches over the last hour") Double precipitation,
            @JsonPropertyDescription("Current cloud cover percent. Value from 0-100") Integer cloudCover,
            @JsonPropertyDescription("Current sea-level pressure in hPa") Double pressure,
            @JsonPropertyDescription("Whether it is currently daytime (1) or nighttime (0)") Integer isDay,
            @JsonPropertyDescription("Current conditions description") String conditions
    ) {
    }

    @Builder
    public record Day(
            @JsonPropertyDescription("Forecast date in YYYY-MM-DD format") String date,
            @JsonPropertyDescription("Forecast conditions description") String conditions,
            @JsonPropertyDescription("Maximum temperature in Fahrenheit") Integer maxTemperature,
            @JsonPropertyDescription("Minimum temperature in Fahrenheit") Integer minTemperature,
            @JsonPropertyDescription("Maximum probability of precipitation in percent, 0-100") Integer precipitationChance,
            @JsonPropertyDescription("Total expected precipitation in inches") Double precipitation,
            @JsonPropertyDescription("Maximum UV index") Double uvIndex,
            @JsonPropertyDescription("Sunrise time in HH:MM format") String sunrise,
            @JsonPropertyDescription("Sunset time in HH:MM format") String sunset
    ) {
    }
}
