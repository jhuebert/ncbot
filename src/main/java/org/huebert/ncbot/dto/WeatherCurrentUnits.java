package org.huebert.ncbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherCurrentUnits(
        @JsonProperty("time") String time,
        @JsonProperty("interval") String interval,
        @JsonProperty("temperature_2m") String temperature2m,
        @JsonProperty("relative_humidity_2m") String relativeHumidity2m,
        @JsonProperty("weather_code") String weatherCode,
        @JsonProperty("wind_speed_10m") String windSpeed10m,
        @JsonProperty("wind_direction_10m") String windDirection10m
) {
}
