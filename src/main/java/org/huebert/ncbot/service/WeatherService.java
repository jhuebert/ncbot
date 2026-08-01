package org.huebert.ncbot.service;

import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.config.NcbotProperties;
import org.huebert.ncbot.dto.WeatherApiResponse;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Service
public class WeatherService {

    private static final String WEATHER_BASE = "https://api.open-meteo.com/v1/forecast";
    private static final String CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m";

    private final RestClient restClient;

    public WeatherService(NcbotProperties properties) {
        // Bounded timeouts: a hung weather lookup must not stall the AI tool
        // call indefinitely. Value from ncbot.weather-timeout (default 10s).
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) properties.weatherTimeout().toMillis();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @DebugLog
    public Optional<WeatherApiResponse> getWeather(double latitude, double longitude) {
        ResponseEntity<WeatherApiResponse> weatherResponse = restClient.get()
                .uri(WEATHER_BASE + "?latitude={lat}&longitude={lon}&current={fields}",
                        latitude, longitude, CURRENT_FIELDS)
                .retrieve()
                .toEntity(WeatherApiResponse.class);
        if (weatherResponse.getStatusCode() != HttpStatus.OK) {
            log.error("error encountered during weather fetch: {}", weatherResponse);
            return Optional.empty();
        }
        return Optional.ofNullable(weatherResponse.getBody());
    }

}
