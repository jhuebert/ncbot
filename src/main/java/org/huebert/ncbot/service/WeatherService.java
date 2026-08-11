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
    private static final String CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,"
                    + "weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m";
    private static final String DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,"
                    + "precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max";

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
        // Requests explicit US units so no client-side conversion is needed, plus a 7-day
        // daily forecast, location timezone, and the extended set of current observations.
        ResponseEntity<WeatherApiResponse> weatherResponse = restClient.get()
                .uri(WEATHER_BASE + "?latitude={lat}&longitude={lon}"
                                + "&current={current}&daily={daily}"
                                + "&temperature_unit=fahrenheit&wind_speed_unit=mph"
                                + "&precipitation_unit=inch&forecast_days=7&timezone=auto",
                        latitude, longitude, CURRENT_FIELDS, DAILY_FIELDS)
                .retrieve()
                .toEntity(WeatherApiResponse.class);
        if (weatherResponse.getStatusCode() != HttpStatus.OK) {
            log.error("error encountered during weather fetch: {}", weatherResponse);
            return Optional.empty();
        }
        return Optional.ofNullable(weatherResponse.getBody());
    }

}
