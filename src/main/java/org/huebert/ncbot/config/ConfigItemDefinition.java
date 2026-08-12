package org.huebert.ncbot.config;

import java.util.Comparator;
import java.util.Locale;

/**
 * Registry of every configuration item ncbot understands. This is the single,
 * code-level source of truth for configuration: keys, types, defaults, user-facing
 * descriptions, and whether a change requires an application restart to take effect.
 * <p>
 * Adding a new runtime configuration item is a one-line change here — the database
 * row is seeded automatically on startup with {@link #defaultValue()} and the admin
 * UI surfaces it without further wiring.
 * <p>
 * Keys use dot-separated names (e.g. {@code bot.name}, {@code channels.ai-each}).
 */
public enum ConfigItemDefinition {

    // ── Bot identity & prompts ─────────────────────────────────────────

    NAME(
            "bot.name",
            ConfigType.STRING,
            "ncbot",
            "Bot display name. Used for @[name] tags and general identification.",
            false),

    SYSTEM_PROMPT(
            "bot.system-prompt",
            ConfigType.TEXT,
            """
            You are ncbot, a helpful and witty AI assistant on the Meshcore network.

            Rules:
            - Response must be ≤128 UTF-8 bytes.
            - Always tag users using the format @[username] (e.g. @[nc1v]).
            - Do not prefix with "ncbot:" or your name.
            - Use __CHAT_MEMORY__ (key=value) as factual knowledge; never repeat keys/values in your response.
            - __CHAT_MESSAGES__ is a JSON array of messages, oldest first. Each entry is
              {"sender":..., "message":..., "timestamp":..., "age":..., "response":...}; the last entry is
              the current message. Multi-line messages are escaped JSON strings.
            - Tools: only call a tool when the user explicitly asks and the data is not already fresh in context.
              Time-sensitive claims in older messages (weather, status, prices) are stale — use each message's
              "age"/"timestamp" to judge; if a claim is older than ~15 minutes, call the tool for fresh data.
              Tool calls are slow — otherwise prefer memory and conversation context. If a tool returns nothing, say so briefly.
            - Memory tools: use insertMemory/updateMemory/deleteMemory to persist durable user facts (preferences,
              identity, decisions, rules) stated in this conversation; current memories are listed in __CHAT_MEMORY__.
              Do not store ephemeral content (mood, status, current activity, one-off remarks).

            Input blocks: __CHAT_MEMORY__, __CHAT_MESSAGES__, __ADDITIONAL_CONTEXT__
            Output only the response text.
            """,
            "System prompt used for every AI chat reply.",
            false),

    CONDENSE_PROMPT(
            "bot.condense-prompt",
            ConfigType.TEXT,
            """
            Condense __RESPONSE__ to be ≤128 UTF-8 bytes.
            Keep @[username] tags and the essential facts.
            Output only the condensed response.
            """,
            "Prompt used to compress overly long responses.",
            false),

    MEMORY_PROMPT(
            "bot.memory-prompt",
            ConfigType.TEXT,
            """
            You are ncbot's memory manager. Update the long-term memory for the channel shown on the CHANNEL_ID line by calling tools.

            Input:
            - CHANNEL_ID: the numeric id of the current channel — pass this to every channel-scoped tool call.
            - __CHAT_MEMORY__: the current channel+global memories (key=value, one per line). Global memories are read-only.
            - __CHAT_MESSAGES__: JSON array of messages, oldest first; each entry has sender, message, timestamp/age, and an optional response.

            Use these tools to update ONLY durable, useful facts:
            - insertMemory(channelId, key, value): add a new channel memory (fails if the key already exists)
            - updateMemory(channelId, key, value): change an existing channel memory (fails if it does not exist)
            - deleteMemory(channelId, key): remove an existing channel memory (fails if it does not exist)

            Rules:
            - Only store durable facts: user preferences, user relationships,
              ongoing channel jokes/rivalries, decisions, user-defined bot rules.
            - Prune ephemeral memories: current activity, mood, presence, last-seen statements, one-off comments,
              bot response summaries. Prefer deletion over retention.
            - Delete keys with forbidden suffixes: status, mood, greeting, activity, last_*, current_, next_.
            - Limit to at most 5-8 facts per user.
            - Keys are dot-separated (channel.*, bot.*, user.[name].*). Values are dense facts, not sentences.
            - A channel memory with the same key as a global memory overrides it for this channel only — never modify global memory.
            - Make all needed changes in one turn; do not narrate.
            """,
            "Prompt used for scheduled memory synthesis (the model updates memory via tools).",
            false),

    MEMORY_MAX_FAILURES(
            "memory.max-failures",
            ConfigType.INT,
            "3",
            "Consecutive failed memory-synthesis runs per channel before a partition is skipped (cursor advanced) so a deterministically failing batch is not retried forever.",
            false),

    WELCOME_CONTENT(
            "chat.welcome-content",
            ConfigType.TEXT,
            "",
            "Content shown to brand-new participants in welcome channels. Leave empty for a generic welcome.",
            false),

    // ── Chat behaviour ─────────────────────────────────────────────────

    MAX_CHAT_HISTORY(
            "chat.max-history",
            ConfigType.INT,
            "25",
            "Maximum number of recent messages included in an AI chat prompt.",
            false),

    AI_ENABLED(
            "chat.ai-enabled",
            ConfigType.BOOLEAN,
            "true",
            "Master switch for the AI. When false, no channel resolves to an AI mode.",
            false),

    AUTO_UPDATE_MEMORY(
            "chat.auto-update-memory",
            ConfigType.BOOLEAN,
            "true",
            "Whether scheduled memory synthesis runs automatically.",
            false),

    USE_MEMORY(
            "chat.use-memory",
            ConfigType.BOOLEAN,
            "true",
            "Whether stored memories are included in AI chat prompts.",
            false),

    CONDENSE(
            "chat.condense",
            ConfigType.BOOLEAN,
            "true",
            "Whether responses longer than the byte limit are condensed by a second AI call.",
            false),

    ALLOW_ONE_BYTE_PATHS(
            "chat.allow-one-byte-paths",
            ConfigType.BOOLEAN,
            "true",
            "Whether messages on 1-byte paths reach command and AI handlers.",
            false),

    PATH_UPGRADE_COOLDOWN_MINUTES(
            "chat.path-upgrade-cooldown-minutes",
            ConfigType.INT,
            "1440",
            "Cooldown in minutes between path-upgrade notifications for a participant.",
            false),

    MINIMUM_RESPONSE_MS(
            "chat.minimum-response-ms",
            ConfigType.LONG,
            "0",
            "Minimum response delay in milliseconds (0 = none).",
            false),

    MAX_REPLY_BYTES(
            "chat.max-reply-bytes",
            ConfigType.INT,
            "128",
            "Maximum UTF-8 byte length of a reply.",
            false),

    MAX_REPLY_TOKENS(
            "chat.max-reply-tokens",
            ConfigType.INT,
            "256",
            "Maximum output tokens allowed per AI call.",
            true),

    // ── Memory system ──────────────────────────────────────────────────

    MEMORY_PARTITION_SIZE(
            "memory.partition-size",
            ConfigType.INT,
            "100",
            "Number of messages sent to the model in each memory-synthesis partition.",
            false),

    // ── Channel routing ────────────────────────────────────────────────

    CHANNELS_WELCOME(
            "channels.welcome",
            ConfigType.STRING,
            "^#ncbot$",
            "Regex for channels where new participants are welcomed.",
            false),

    CHANNELS_COMMAND(
            "channels.command",
            ConfigType.STRING,
            "^#ncbot$",
            "Regex for channels where shortcut commands are enabled.",
            false),

    CHANNELS_PATH_UPGRADE(
            "channels.path-upgrade",
            ConfigType.STRING,
            "^#ncbot$",
            "Regex for channels where path-upgrade notices are shown.",
            false),

    CHANNELS_AI_EACH(
            "channels.ai-each",
            ConfigType.STRING,
            "^#ncbot$",
            "Regex for channels where AI responds to every message.",
            false),

    CHANNELS_AI_TAGGED(
            "channels.ai-tagged",
            ConfigType.STRING,
            ".*",
            "Regex for channels where AI responds only when the bot is tagged.",
            false),

    ALLOWED_DMS(
            "channels.allowed-dms",
            ConfigType.LIST,
            "",
            "Comma-separated sender keys allowed to DM the bot. Empty list = all blocked.",
            false),

    // ── Blocking & filtering ───────────────────────────────────────────

    BLOCK_USER(
            "blocking.block-user",
            ConfigType.STRING,
            "",
            "Regex of user names to block. Allow patterns take precedence.",
            false),

    ALLOW_USER(
            "blocking.allow-user",
            ConfigType.STRING,
            "",
            "Regex of user names always allowed, regardless of block patterns.",
            false),

    BLOCK_PATH(
            "blocking.block-path",
            ConfigType.STRING,
            "",
            "Regex of paths to block.",
            false),

    ALLOW_PATH(
            "blocking.allow-path",
            ConfigType.STRING,
            "",
            "Regex of paths always allowed, regardless of block patterns.",
            false),

    BLOCK_CHANNEL(
            "blocking.block-channel",
            ConfigType.STRING,
            "",
            "Regex of channel names to block (non-DM only).",
            false),

    ALLOW_CHANNEL(
            "blocking.allow-channel",
            ConfigType.STRING,
            "",
            "Regex of channel names always allowed, regardless of block patterns.",
            false);

    private final String key;
    private final ConfigType type;
    private final String defaultValue;
    private final String description;
    private final boolean restartRequired;

    ConfigItemDefinition(String key, ConfigType type, String defaultValue, String description, boolean restartRequired) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
        this.restartRequired = restartRequired;
    }

    public String key() {
        return key;
    }

    public ConfigType type() {
        return type;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String description() {
        return description;
    }

    public boolean restartRequired() {
        return restartRequired;
    }

    /** Look up a definition by its dot-separated key. */
    public static ConfigItemDefinition fromKey(String key) {
        for (ConfigItemDefinition def : values()) {
            if (def.key.equalsIgnoreCase(key)) {
                return def;
            }
        }
        throw new IllegalArgumentException("Unknown configuration item: " + key);
    }

    /** Provides a stable, grouped ordering for the admin UI. */
    public static Comparator<ConfigItemDefinition> byKeyOrder() {
        return Comparator.comparing(ConfigItemDefinition::key);
    }

    @Override
    public String toString() {
        return key + " (" + type.name().toLowerCase(Locale.ROOT) + ")";
    }
}
