package org.huebert.ncbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherCurrent(
        @JsonProperty("time") String time,
        @JsonProperty("interval") Integer interval,
        @JsonProperty("temperature_2m") Double temperature2m,
        @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
        @JsonProperty("weather_code") Integer weatherCode,
        @JsonProperty("wind_speed_10m") Double windSpeed10m,
        @JsonProperty("wind_direction_10m") Integer windDirection10m
) {
}
