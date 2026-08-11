package org.huebert.ncbot.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.dto.WeatherApiResponse;
import org.huebert.ncbot.dto.WeatherCode;
import org.huebert.ncbot.dto.WeatherToolResponse;
import org.huebert.ncbot.service.WeatherService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTool {

    private final WeatherService weatherService;

    @DebugLog
    @Tool(description = "Get current weather and the short-term (7-day) forecast for a location. "
            + "Temperature in degrees Fahrenheit, wind speed in mph, precipitation in inches, "
            + "pressure in hPa. Returns current observations (temperature, feels-like, wind, gusts, "
            + "humidity, precipitation, cloud cover, pressure) plus a per-day forecast (high/low, "
            + "conditions, precipitation chance, UV index, sunrise/sunset) for the next 7 days. "
            + "Call only when the user asks about weather and the data is not already fresh in the "
            + "conversation (a weather claim older than ~15 minutes is stale).")
    public WeatherToolResponse getWeather(
            @ToolParam(description = "Latitude in decimal degrees (e.g. 40.7128). Estimate from the "
                    + "location name if the exact value is unknown.") double latitude,
            @ToolParam(description = "Longitude in decimal degrees (e.g. -74.0060). Estimate from "
                    + "the location name if the exact value is unknown.") double longitude
    ) {
        return weatherService.getWeather(latitude, longitude)
                .map(r -> WeatherToolResponse.builder()
                        .latitude(r.latitude())
                        .longitude(r.longitude())
                        .timezone(r.timezone())
                        .current(toCurrent(r))
                        .forecast(toForecast(r))
                        .build())
                .orElse(null);
    }

    private static WeatherToolResponse.Current toCurrent(WeatherApiResponse r) {
        return WeatherToolResponse.Current.builder()
                .conditions(WeatherCode.fromCode(r.current().weatherCode()).getDescription())
                .temperature(getTemperature(r, "°C".equals(r.currentUnits().temperature2m()),
                        r.current().temperature2m()))
                .apparentTemperature(getTemperature(r, "°C".equals(r.currentUnits().apparentTemperature()),
                        r.current().apparentTemperature()))
                .humidity(r.current().relativeHumidity2m())
                .windSpeed(getWindSpeed(r, r.current().windSpeed10m()))
                .windGusts(getWindSpeed(r, r.current().windGusts10m()))
                .windDirection(r.current().windDirection10m())
                .precipitation(round(r.current().precipitation()))
                .cloudCover(r.current().cloudCover())
                .pressure(r.current().pressureMsl())
                .isDay(r.current().isDay())
                .build();
    }

    private static List<WeatherToolResponse.Day> toForecast(WeatherApiResponse r) {
        if (r.daily() == null) {
            return List.of();
        }
        List<String> time = r.daily().time();
        int size = time == null ? 0 : time.size();
        boolean celsius = r.dailyUnits() != null && "°C".equals(r.dailyUnits().temperature2mMax());
        List<WeatherToolResponse.Day> days = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            days.add(WeatherToolResponse.Day.builder()
                    .date(time.get(i))
                    .conditions(WeatherCode.fromCode(iValue(r.daily().weatherCode(), i)).getDescription())
                    .maxTemperature(getTemperature(r, celsius, dValue(r.daily().temperature2mMax(), i)))
                    .minTemperature(getTemperature(r, celsius, dValue(r.daily().temperature2mMin(), i)))
                    .precipitationChance(iValue(r.daily().precipitationProbabilityMax(), i))
                    .precipitation(round(dValue(r.daily().precipitationSum(), i)))
                    .uvIndex(dValue(r.daily().uvIndexMax(), i))
                    .sunrise(toTime(sValue(r.daily().sunrise(), i)))
                    .sunset(toTime(sValue(r.daily().sunset(), i)))
                    .build());
        }
        return days;
    }

    private static int getTemperature(WeatherApiResponse r, boolean celsius, double value) {
        Double raw = value;
        if (raw == null) {
            return 0;
        }
        if (celsius) {
            raw = (1.8 * raw) + 32.0;
        }
        return (int) Math.rint(raw);
    }

    private static int getWindSpeed(WeatherApiResponse r, double value) {
        Double raw = value;
        if (raw == null) {
            return 0;
        }
        if (r.currentUnits() != null && "km/h".equals(r.currentUnits().windSpeed10m())) {
            raw = 0.6213711922 * raw;
        }
        return (int) Math.rint(raw);
    }

    private static Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private static Integer iValue(List<Integer> list, int index) {
        return list != null && index < list.size() ? list.get(index) : null;
    }

    private static Double dValue(List<Double> list, int index) {
        return list != null && index < list.size() ? list.get(index) : null;
    }

    private static String sValue(List<String> list, int index) {
        return list != null && index < list.size() ? list.get(index) : null;
    }

    private static String toTime(String iso) {
        if (iso == null || iso.length() < 16) {
            return iso;
        }
        return iso.substring(11, 16);
    }

}
