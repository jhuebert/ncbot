package org.huebert.ncbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherDailyUnits(
        @JsonProperty("time") String time,
        @JsonProperty("weather_code") String weatherCode,
        @JsonProperty("temperature_2m_max") String temperature2mMax,
        @JsonProperty("temperature_2m_min") String temperature2mMin,
        @JsonProperty("apparent_temperature_max") String apparentTemperatureMax,
        @JsonProperty("apparent_temperature_min") String apparentTemperatureMin,
        @JsonProperty("sunrise") String sunrise,
        @JsonProperty("sunset") String sunset,
        @JsonProperty("uv_index_max") String uvIndexMax,
        @JsonProperty("precipitation_sum") String precipitationSum,
        @JsonProperty("precipitation_probability_max") String precipitationProbabilityMax,
        @JsonProperty("wind_speed_10m_max") String windSpeed10mMax,
        @JsonProperty("wind_gusts_10m_max") String windGusts10mMax
) {
}
