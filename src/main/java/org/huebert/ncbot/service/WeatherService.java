package org.huebert.ncbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class WeatherService {

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.create();
    }

    /**
     * Fetch weather for a location using Open-Meteo (free, no API key).
     * First geocodes the location, then fetches weather data.
     */
    public Optional<String> getWeather(String location) {
        // Step 1: Geocode
        ResponseEntity<Map<String, Object>> geoResponse = restClient.get()
                .uri("https://geocoding-api.open-meteo.com/v1/search?name={query}&count=1", location)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                });
        log.debug("geoResponse: {}", geoResponse);

        if (geoResponse.getStatusCode() != HttpStatus.OK) {
            log.error("error encountered during geocoding: {}", geoResponse);
            return Optional.empty();
        }

        Map<String, Object> geoBody = geoResponse.getBody();
        if (geoBody == null || !geoBody.containsKey("results")) {
            log.error("empty geocoding body");
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        var results = (java.util.List<Map<String, Object>>) geoBody.get("results");
        if (results == null || results.isEmpty()) {
            log.error("empty geocoding results");
            return Optional.empty();
        }

        Map<String, Object> firstResult = results.get(0);
        double latitude = ((Number) firstResult.get("latitude")).doubleValue();
        double longitude = ((Number) firstResult.get("longitude")).doubleValue();
        String name = (String) firstResult.get("name");

        ResponseEntity<Map<String, Object>> weatherResponse = restClient.get()
                .uri("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}"
                                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,"
                                + "precipitation,weather_code,wind_speed_10m,wind_direction_10m",
                        latitude, longitude)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        if (weatherResponse.getStatusCode() != HttpStatus.OK) {
            log.error("error encountered during weather fetch: {}", weatherResponse);
            return Optional.empty();
        }

        Map<String, Object> weatherBody = weatherResponse.getBody();
        if (weatherBody == null || !weatherBody.containsKey("current")) {
            log.error("empty weather body");
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> current = (Map<String, Object>) weatherBody.get("current");

        int temp = (int) Math.rint((((Number) current.get("temperature_2m")).doubleValue()) * 1.8 + 32.0);
        int humidity = ((Number) current.get("relative_humidity_2m")).intValue();
        int windSpeed = (int) Math.rint(((Number) current.get("wind_speed_10m")).doubleValue() * 0.6213711922);
        int weatherCode = ((Number) current.get("weather_code")).intValue();

        String condition = weatherCodeToString(weatherCode);
        return Optional.of(String.format("%s: %d°F, %s, %d%% humidity, %d mph wind",
                name, temp, condition, humidity, windSpeed));
    }

    private String weatherCodeToString(int code) {
        return switch (code) {
            case 0 -> "clear";
            case 1 -> "mostly clear";
            case 2 -> "partly cloudy";
            case 3 -> "overcast";
            case 45 -> "fog";
            case 48 -> "icy fog";
            case 51 -> "light drizzle";
            case 53 -> "drizzle";
            case 55 -> "heavy drizzle";
            case 56 -> "light freezing drizzle";
            case 57 -> "freezing drizzle";
            case 61 -> "light rain";
            case 63 -> "rain";
            case 65 -> "heavy rain";
            case 66 -> "light freezing rain";
            case 67 -> "freezing rain";
            case 71 -> "light snow";
            case 73 -> "snow";
            case 75 -> "heavy snow";
            case 77 -> "snow grains";
            case 80 -> "light showers";
            case 81 -> "showers";
            case 82 -> "heavy showers";
            case 85 -> "light snow showers";
            case 86 -> "snow showers";
            case 95 -> "thunderstorm";
            case 96 -> "light thunderstorm with hail";
            case 99 -> "thunderstorm with hail";
            default -> "code " + code;
        };
    }
}
