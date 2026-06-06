package org.huebert.ncbot.handler.command;

import org.huebert.ncbot.dto.ChatRequest;

import java.util.Map;
import java.util.regex.Pattern;

public interface CommandHandler {

    Pattern getPattern();

    Map<String, Object> handle(ChatRequest request, Map<String, String> groups);

    //TODO Add Wx|weather with location (?:wx|weather|w)(?:\s+(?<location>.+))?
    //TODO Forecast
    //TODO Random with start and end or up to or float
    //TODO Lottery

}
