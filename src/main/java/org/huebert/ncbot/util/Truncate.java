package org.huebert.ncbot.util;

import com.google.common.base.Utf8;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class Truncate {

    private static final String PUNCTUATION_REGEX = "[\\p{P}\\p{S}]";
    private static final String EXTRA_WHITESPACE_REGEX = "\\s+";

    public static String truncateUtf8(String text, int maxBytes) {

        if (Utf8.encodedLength(text) <= maxBytes) {
            return text;
        }

        text = text.replaceAll(PUNCTUATION_REGEX, "").trim();
        if (Utf8.encodedLength(text) <= maxBytes) {
            return text;
        }

        text = text.replaceAll(EXTRA_WHITESPACE_REGEX, " ").trim();
        if (Utf8.encodedLength(text) <= maxBytes) {
            return text;
        }

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int limit = maxBytes - 3;
        while ((bytes[limit] & 0xC0) == 0x80) {
            limit--;
        }

        return new String(bytes, 0, limit, StandardCharsets.UTF_8) + "...";
    }

    public static String joinWithLimit(List<String> items, int maxBytes, String separator) {
        int sepBytes = Utf8.encodedLength(separator);
        StringBuilder text = new StringBuilder();
        boolean truncated = false;

        for (String item : items) {
            boolean needsSeparator = !text.isEmpty();
            int added = Utf8.encodedLength(item) + (needsSeparator ? sepBytes : 0);
            if (Utf8.encodedLength(text.toString()) + added > maxBytes) {
                truncated = true;
                break;
            }
            if (needsSeparator) text.append(separator);
            text.append(item);
        }

        if (truncated) {
            while (Utf8.encodedLength(text.toString()) + sepBytes + 3 > maxBytes) {
                int lastSep = text.lastIndexOf(separator);
                if (lastSep == -1) { text.setLength(0); break; }
                text.setLength(lastSep);
            }
            text.append(separator).append("...");
        }

        return text.toString();
    }

}
