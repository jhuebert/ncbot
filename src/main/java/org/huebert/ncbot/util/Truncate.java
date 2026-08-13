package org.huebert.ncbot.util;

import com.google.common.base.Utf8;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

public class Truncate {

    private static final Pattern EXTRA_WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Trim a string to at most {@code maxBytes} UTF-8 bytes, preserving as much meaning as
     * possible. Never strips punctuation or tags globally (that mangles $109/<5W/@[tag]);
     * instead it collapses runs of whitespace and, if still over budget, cuts at the last
     * whole UTF-8 character/word boundary, appending an ellipsis.
     */
    public static String truncateUtf8(String text, int maxBytes) {
        if (Utf8.encodedLength(text) <= maxBytes) {
            return text;
        }

        // 1) Collapse runs of whitespace first — cheap and meaning-preserving.
        String compact = EXTRA_WHITESPACE_PATTERN.matcher(text).replaceAll(" ").trim();
        if (Utf8.encodedLength(compact) <= maxBytes) {
            return compact;
        }

        // 2) Otherwise keep as much as fits from the front, ending at a whole UTF-8
        //    character and a word boundary, then append an ellipsis.
        byte[] bytes = compact.getBytes(StandardCharsets.UTF_8);
        int limit = maxBytes - 3; // reserve room for the trailing "..."
        if (limit <= 0) {
            return "";
        }

        // Don't split a multi-byte UTF-8 character.
        while (limit > 0 && limit < bytes.length && (bytes[limit] & 0xC0) == 0x80) {
            limit--;
        }
        // Don't cut mid-word: back up to a whitespace boundary.
        while (limit > 0 && limit < bytes.length && bytes[limit] != ' ') {
            limit--;
        }
        // Drop the single trailing space that now sits just past the cut.
        if (limit > 0 && bytes[limit - 1] == ' ') {
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
