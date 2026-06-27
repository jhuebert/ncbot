package org.huebert.ncbot.util;

import org.apache.logging.log4j.util.Strings;

public class PatternUtil {

    public static boolean matches(String value, String pattern) {
        if ((value == null) || Strings.isBlank(pattern)) {
            return false;
        }
        return value.matches(pattern);
    }

}
