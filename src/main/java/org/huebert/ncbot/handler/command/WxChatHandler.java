package org.huebert.ncbot.handler.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.dto.ChatRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * {code wx}/{code weather} — answers a weather request through the AI model rather than
 * fulfilling it directly. Because the reply is AI-generated, this works in channels
 * where AI mode is {@code DISABLED} or where ncbot was not tagged.
 * <p>
 * A location can be appended (ZIP code, city name, coordinates, ...) and is handed off
 * to the model via the WeatherTool; it is converted to coordinates by the model.
 * <p>
 * This handler returns the {@code ai} flag so
 * {@link org.huebert.ncbot.handler.CommandChatHandler} routes the reply through
 * {@link org.huebert.ncbot.service.AiService}.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class WxChatHandler implements CommandHandler {

    private static final Pattern PATTERN = Pattern.compile(
            "^(?:wx|weather)(?:\\s+(?<location>.+))?$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public Map<String, Object> handle(ChatRequest request, Map<String, String> groups) {
        String location = Strings.trimToNull(groups.get("location"));
        String prompt = location == null
                ? "The user asked for the weather but gave no location. If a default or home "
                + "location exists in chat memory or a recent message, use it. Otherwise, "
                + "briefly ask the user what location they'd like the weather for, and "
                + "tell them to reply with the full command including the location, e.g. "
                + "'wx <location>' or 'weather <location>', so the request is recognized as "
                + "a weather lookup rather than a bare location name."
                : "Give the current weather and a short 7-day forecast for: " + location
                + ". Convert the location (city name, ZIP code, coordinates, etc.) to "
                + "latitude/longitude and call the getWeather tool. Reply concisely.";
        return Map.of("ai", true, "aiMessage", prompt);
    }

}
