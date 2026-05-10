package org.huebert.ncbot;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Delay {

    public static boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            log.error("interrupted", e);
            return false;
        }
    }
}
