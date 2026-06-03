package org.huebert.ncbot.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum WeatherCode {
    UNKNOWN(-1, "unknown"),
    CLEAR(0, "clear"),
    MOSTLY_CLEAR(1, "mostly clear"),
    PARTLY_CLOUDY(2, "partly cloudy"),
    OVERCAST(3, "overcast"),
    FOG(45, "fog"),
    ICY_FOG(48, "icy fog"),
    LIGHT_DRIZZLE(51, "light drizzle"),
    DRIZZLE(53, "drizzle"),
    HEAVY_DRIZZLE(55, "heavy drizzle"),
    LIGHT_FREEZING_DRIZZLE(56, "light freezing drizzle"),
    FREEZING_DRIZZLE(57, "freezing drizzle"),
    LIGHT_RAIN(61, "light rain"),
    RAIN(63, "rain"),
    HEAVY_RAIN(65, "heavy rain"),
    LIGHT_FREEZING_RAIN(66, "light freezing rain"),
    FREEZING_RAIN(67, "freezing rain"),
    LIGHT_SNOW(71, "light snow"),
    SNOW(73, "snow"),
    HEAVY_SNOW(75, "heavy snow"),
    SNOW_GRAINS(77, "snow grains"),
    LIGHT_SHOWERS(80, "light showers"),
    SHOWERS(81, "showers"),
    HEAVY_SHOWERS(82, "heavy showers"),
    LIGHT_SNOW_SHOWERS(85, "light snow showers"),
    SNOW_SHOWERS(86, "snow showers"),
    THUNDERSTORM(95, "thunderstorm"),
    LIGHT_THUNDERSTORM_WITH_HAIL(96, "light thunderstorm with hail"),
    THUNDERSTORM_WITH_HAIL(99, "thunderstorm with hail");

    private static final Map<Integer, WeatherCode> BY_CODE;

    private final int code;
    private final String description;

    static {
        Map<Integer, WeatherCode> map = new HashMap<>();
        for (WeatherCode code : values()) {
            map.put(code.getCode(), code);
        }
        BY_CODE = Map.copyOf(map);
    }

    public static WeatherCode fromCode(int code) {
        return BY_CODE.getOrDefault(code, UNKNOWN);
    }

}
