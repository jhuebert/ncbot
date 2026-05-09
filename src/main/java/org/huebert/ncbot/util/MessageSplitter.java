package org.huebert.ncbot.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits text into chunks that each fit within a byte limit.
 * Splits at sentence boundaries when possible.
 */
public class MessageSplitter {

    public static List<String> splitIntoMessages(String text, int maxBytes) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return List.of(text);
        }

        List<String> messages = new ArrayList<>();
        String remaining = text;

        while (!remaining.isEmpty()) {
            byte[] remainingBytes = remaining.getBytes(StandardCharsets.UTF_8);
            if (remainingBytes.length <= maxBytes) {
                messages.add(remaining);
                break;
            }

            // Try to find a sentence boundary within the limit
            String candidate = truncateToFit(remaining, maxBytes);
            messages.add(candidate);
            remaining = remaining.substring(candidate.length()).trim();
        }

        return messages;
    }

    private static String truncateToFit(String text, int maxBytes) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return text;
        }

        // Try to split at sentence boundary within limit
        String truncated = safeTruncate(text, maxBytes - 3); // reserve space for "..."
        String[] sentences = truncated.split("(?<=[.!?])\\s+", -1);

        StringBuilder result = new StringBuilder();
        for (String sentence : sentences) {
            String candidate = result.isEmpty() ? sentence.trim() : result + " " + sentence.trim();
            if (candidate.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
                break;
            }
            result = new StringBuilder(candidate);
        }

        if (result.length() < text.length()) {
            result.append("...");
        }

        return result.toString().trim();
    }

    private static String safeTruncate(String text, int maxBytes) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return text;
        }

        int end = maxBytes;
        // Make sure we don't cut in the middle of a UTF-8 character
        while (end > 0 && (bytes[end] & 0xC0) == 0x80) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}
