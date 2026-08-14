package org.huebert.ncbot;

import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.handler.command.WxChatHandler;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WxChatHandlerTest {

    private static final ChatRequest REQUEST = new ChatRequest(
            "nc1v", "abc123", "wx", false, "#ncbot", "#ncbot",
            null, null, null, null, null, "lora", null
    );

    private final WxChatHandler handler = new WxChatHandler();

    private Map<String, String> groups(String message) {
        Pattern pattern = handler.getPattern();
        Matcher matcher = pattern.matcher(message);
        assertTrue(matcher.matches(), "expected match for: " + message);
        Map<String, String> map = new HashMap<>();
        pattern.namedGroups().keySet().forEach(name -> map.put(name, matcher.group(name)));
        return map;
    }

    @Test
    void matchesAliasesAndLocations() {
        for (String msg : new String[]{"wx", "weather", "WX", "Weather", "wx 68123", "weather Chicago", "Wx Omaha, NE"}) {
            assertTrue(handler.getPattern().matcher(msg).matches(), "no match: " + msg);
        }
        for (String msg : new String[]{"", "wxchicago", "forecast", "help"}) {
            assertFalse(handler.getPattern().matcher(msg).matches(), "unexpected match: " + msg);
        }
    }

    @Test
    void noLocationAsksForIt() {
        Map<String, Object> result = handler.handle(REQUEST, groups("wx"));
        assertEquals(true, result.get("ai"));
        String prompt = result.get("aiMessage").toString();
        assertTrue(prompt.toLowerCase().contains("no location"), prompt);
        // The reply must tell the user to re-invoke the command with the location,
        // otherwise a bare location-only reply wouldn't be recognized (AI may not be EACH).
        assertTrue(prompt.contains("'wx <location>'"), prompt);
    }

    @Test
    void capturesLocation() {
        Map<String, Object> result = handler.handle(REQUEST, groups("wx 68123"));
        assertEquals(true, result.get("ai"));
        String prompt = result.get("aiMessage").toString();
        assertTrue(prompt.contains("68123"), prompt);
    }

    @Test
    void locationGroupIsNullWhenAbsent() {
        assertNull(groups("weather").get("location"));
    }
}
