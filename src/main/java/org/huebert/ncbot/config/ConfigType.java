package org.huebert.ncbot.config;

/**
 * The logical type of a configuration item, used to validate input and to render
 * the correct editor control in the admin UI.
 */
public enum ConfigType {

    /** Single-line string. */
    STRING,

    /** Multi-line free-form text (prompts, content). */
    TEXT,

    /** {@code true}/{@code false}. */
    BOOLEAN,

    /** 32-bit integer. */
    INT,

    /** 64-bit integer. */
    LONG,

    /** Comma-separated list of strings. */
    LIST

}
