package org.huebert.ncbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherApiResponse(
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("generation_time_ms") Double generationtimeMs,
        @JsonProperty("utc_offset_seconds") Integer utcOffsetSeconds,
        @JsonProperty("timezone") String timezone,
        @JsonProperty("timezone_abbreviation") String timezoneAbbreviation,
        @JsonProperty("elevation") Double elevation,
        @JsonProperty("current_units") WeatherCurrentUnits currentUnits,
        @JsonProperty("current") WeatherCurrent current
) {
}
