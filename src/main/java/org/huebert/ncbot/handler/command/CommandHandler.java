package org.huebert.ncbot.handler.command;

import org.huebert.ncbot.dto.ChatRequest;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * A shortcut command handler. {@link #handle(ChatRequest, Map)} returns a map that
 * {@code CommandChatHandler} interprets:
 * <ul>
 *     <li>{@code template} — a jte template name rendered with the request and any other
 *     map entries as params (standard commands).</li>
 *     <li>{@code ai: true} — routes the reply through the AI model instead of a template,
 *     bypassing the channel's AI mode gating (e.g. {@code wx}/{@code weather}). An optional
 *     {@code aiMessage} string overrides the user's raw text fed to the model.</li>
 * </ul>
 */
public interface CommandHandler {

    Pattern getPattern();

    Map<String, Object> handle(ChatRequest request, Map<String, String> groups);

}
