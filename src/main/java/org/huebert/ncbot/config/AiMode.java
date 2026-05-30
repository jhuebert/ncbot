package org.huebert.ncbot.config;

public enum AiMode {

    /** AI is completely disabled for this channel */
    DISABLED,

    /** AI responds to every message */
    EACH,

    /** AI responds only when @bot is mentioned in the message */
    TAGGED
}
