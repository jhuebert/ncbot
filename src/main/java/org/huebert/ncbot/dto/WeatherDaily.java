package org.huebert.ncbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WeatherDaily(
        @JsonProperty("time") List<String> time,
        @JsonProperty("weather_code") List<Integer> weatherCode,
        @JsonProperty("temperature_2m_max") List<Double> temperature2mMax,
        @JsonProperty("temperature_2m_min") List<Double> temperature2mMin,
        @JsonProperty("apparent_temperature_max") List<Double> apparentTemperatureMax,
        @JsonProperty("apparent_temperature_min") List<Double> apparentTemperatureMin,
        @JsonProperty("sunrise") List<String> sunrise,
        @JsonProperty("sunset") List<String> sunset,
        @JsonProperty("uv_index_max") List<Double> uvIndexMax,
        @JsonProperty("precipitation_sum") List<Double> precipitationSum,
        @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbabilityMax,
        @JsonProperty("wind_speed_10m_max") List<Double> windSpeed10mMax,
        @JsonProperty("wind_gusts_10m_max") List<Double> windGusts10mMax
) {
}
