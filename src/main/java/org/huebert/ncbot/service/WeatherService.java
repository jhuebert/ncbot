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

        int temp = ((Number) current.get("temperature_2m")).intValue();
        int humidity = ((Number) current.get("relative_humidity_2m")).intValue();
        int windSpeed = ((Number) current.get("wind_speed_10m")).intValue();
        int weatherCode = ((Number) current.get("weather_code")).intValue();

        String condition = weatherCodeToString(weatherCode);
        return Optional.of(String.format("%s: %d°C, %s, %d%% humidity, %d km/h wind",
                name, temp, condition, humidity, windSpeed));
    }

    private String weatherCodeToString(int code) {
        return switch (code) {
            case 0 -> "clear sky";
            case 1, 2, 3 -> "partly cloudy";
            case 45, 48 -> "foggy";
            case 51, 53, 55 -> "light drizzle";
            case 56, 57 -> "freezing drizzle";
            case 61, 63, 65 -> "rain";
            case 66, 67 -> "freezing rain";
            case 71, 73, 75 -> "snow";
            case 77 -> "snow grains";
            case 80, 81, 82 -> "rain showers";
            case 85, 86 -> "snow showers";
            case 95, 96, 99 -> "thunderstorm";
            default -> "code " + code;
        };
    }
}
