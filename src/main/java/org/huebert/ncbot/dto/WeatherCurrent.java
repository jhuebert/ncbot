package org.huebert.ncbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherCurrent(
        @JsonProperty("time") String time,
        @JsonProperty("interval") Integer interval,
        @JsonProperty("temperature_2m") Double temperature2m,
        @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
        @JsonProperty("apparent_temperature") Double apparentTemperature,
        @JsonProperty("is_day") Integer isDay,
        @JsonProperty("precipitation") Double precipitation,
        @JsonProperty("weather_code") Integer weatherCode,
        @JsonProperty("cloud_cover") Integer cloudCover,
        @JsonProperty("pressure_msl") Double pressureMsl,
        @JsonProperty("wind_speed_10m") Double windSpeed10m,
        @JsonProperty("wind_direction_10m") Integer windDirection10m,
        @JsonProperty("wind_gusts_10m") Double windGusts10m
) {
}
