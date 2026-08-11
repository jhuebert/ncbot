package org.huebert.ncbot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.huebert.ncbot.config.AiMode;
import org.huebert.ncbot.config.ChannelCapabilities;
import org.huebert.ncbot.config.ConfigItemDefinition;
import org.huebert.ncbot.config.ConfigType;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.entity.ConfigItem;
import org.huebert.ncbot.repository.ConfigItemRepository;
import org.huebert.ncbot.util.PatternUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The runtime source of truth for DB-backed configuration. Every code-defined
 * {@link ConfigItemDefinition} is seeded into the {@code config_item} table on
 * startup with its sensible default, and all reads route through this service so
 * the effective value is always the stored one (falling back to the definition
 * default only if a row is absent).
 * <p>
 * Values are validated against their declared type on write and exposed to the
 * admin UI (with descriptions and defaults) via {@link #list()}.
 * <p>
 * Items consumed only at startup (AI/weather/scheduler timeouts, Spring AI
 * credentials) intentionally remain in {@code application.yml} and are described
 * in the documentation — see {@code restartRequired} for items that are stored
 * in the DB but only take effect on restart.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    /** Capabilities cache, invalidated whenever any channel/config changes. */
    private final Map<String, ChannelCapabilities> channelCapabilities = new ConcurrentHashMap<>();

    private static final ChannelCapabilities DM_CAPABILITIES = ChannelCapabilities.builder()
            .welcome(false)
            .pathUpgrade(false)
            .command(true)
            .ai(AiMode.EACH)
            .build();

    private final ConfigItemRepository repository;

    /**
     * Seed every definition that is missing from the DB with its default, so a fresh
     * installation always has a complete, usable configuration table.
     */
    @PostConstruct
    @Transactional
    public void seedDefaults() {
        for (ConfigItemDefinition def : ConfigItemDefinition.values()) {
            if (repository.findById(def.key()).isEmpty()) {
                repository.save(ConfigItem.builder()
                        .key(def.key())
                        .value(def.defaultValue())
                        .build());
                log.info("seeded configuration item {} = {}", def.key(), def.defaultValue());
            }
        }
    }

    // ── Admin listing & mutation ───────────────────────────────────────

    /**
     * All definitions merged with their stored (or default) value, stable order.
     */
    @Transactional(readOnly = true)
    public List<ConfigItemEntry> list() {
        Map<String, ConfigItem> stored = repository.findAll().stream()
                .collect(Collectors.toMap(ConfigItem::getKey, a -> a));
        return java.util.Arrays.stream(ConfigItemDefinition.values())
                .sorted(Comparator.comparing(ConfigItemDefinition::key))
                .map(def -> {
                    ConfigItem item = stored.get(def.key());
                    String value = item != null ? item.getValue() : def.defaultValue();
                    return new ConfigItemEntry(def, value, item == null || def.defaultValue().equals(item.getValue()));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfigItemEntry get(String key) {
        ConfigItemDefinition def = ConfigItemDefinition.fromKey(key);
        return list().stream()
                .filter(e -> e.definition().equals(def))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown configuration item: " + key));
    }

    /**
     * Update a configuration item's value, validating it against its declared type.
     */
    @Transactional
    public ConfigItemEntry update(String key, String newValue) {
        ConfigItemDefinition def = ConfigItemDefinition.fromKey(key);
        String normalized = validate(def, newValue);
        ConfigItem item = repository.findById(def.key()).orElseGet(() ->
                ConfigItem.builder().key(def.key()).build());
        item.setValue(normalized);
        item.setUpdatedAt(Instant.now());
        repository.save(item);
        invalidateCache();
        return get(def.key());
    }

    /**
     * Restore a configuration item to its sensible default (removes the override).
     */
    @Transactional
    public ConfigItemEntry reset(String key) {
        ConfigItemDefinition def = ConfigItemDefinition.fromKey(key);
        repository.findById(def.key()).ifPresent(repository::delete);
        invalidateCache();
        return get(def.key());
    }

    private void invalidateCache() {
        channelCapabilities.clear();
    }

    // ── Typed readers (used by handlers & services) ────────────────────

    public String getString(ConfigItemDefinition def) {
        return raw(def);
    }

    public boolean getBoolean(ConfigItemDefinition def) {
        return Boolean.parseBoolean(raw(def).trim());
    }

    public int getInt(ConfigItemDefinition def) {
        return Integer.parseInt(raw(def).trim());
    }

    public long getLong(ConfigItemDefinition def) {
        return Long.parseLong(raw(def).trim());
    }

    public List<String> getList(ConfigItemDefinition def) {
        String raw = raw(def);
        if (Strings.isBlank(raw)) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String raw(ConfigItemDefinition def) {
        return repository.findById(def.key())
                .map(ConfigItem::getValue)
                .filter(v -> !Strings.isBlank(v))
                .orElse(def.defaultValue());
    }

    // ── Convenience accessors mirroring the old properties surface ─────

    public String name() {
        return getString(ConfigItemDefinition.NAME);
    }

    public String systemPrompt() {
        return getString(ConfigItemDefinition.SYSTEM_PROMPT);
    }

    public String condensePrompt() {
        return getString(ConfigItemDefinition.CONDENSE_PROMPT);
    }

    public String memoryPrompt() {
        return getString(ConfigItemDefinition.MEMORY_PROMPT);
    }

    public String welcomeContent() {
        return getString(ConfigItemDefinition.WELCOME_CONTENT);
    }

    public int maxChatHistory() {
        return getInt(ConfigItemDefinition.MAX_CHAT_HISTORY);
    }

    public boolean aiEnabled() {
        return getBoolean(ConfigItemDefinition.AI_ENABLED);
    }

    public boolean autoUpdateMemory() {
        return getBoolean(ConfigItemDefinition.AUTO_UPDATE_MEMORY);
    }

    public boolean useMemory() {
        return getBoolean(ConfigItemDefinition.USE_MEMORY);
    }

    public boolean condense() {
        return getBoolean(ConfigItemDefinition.CONDENSE);
    }

    public boolean allowOneBytePaths() {
        return getBoolean(ConfigItemDefinition.ALLOW_ONE_BYTE_PATHS);
    }

    public int pathUpgradeCooldownMinutes() {
        return getInt(ConfigItemDefinition.PATH_UPGRADE_COOLDOWN_MINUTES);
    }

    public int memoryPartitionSize() {
        return getInt(ConfigItemDefinition.MEMORY_PARTITION_SIZE);
    }

    public long minimumResponseMs() {
        return getLong(ConfigItemDefinition.MINIMUM_RESPONSE_MS);
    }

    public int maxReplyBytes() {
        return getInt(ConfigItemDefinition.MAX_REPLY_BYTES);
    }

    public int maxReplyTokens() {
        return getInt(ConfigItemDefinition.MAX_REPLY_TOKENS);
    }

    public String welcomeChannelPattern() {
        return getString(ConfigItemDefinition.CHANNELS_WELCOME);
    }

    public String commandChannelPattern() {
        return getString(ConfigItemDefinition.CHANNELS_COMMAND);
    }

    public String pathUpgradeChannelPattern() {
        return getString(ConfigItemDefinition.CHANNELS_PATH_UPGRADE);
    }

    public String aiEachChannelPattern() {
        return getString(ConfigItemDefinition.CHANNELS_AI_EACH);
    }

    public String aiTaggedChannelPattern() {
        return getString(ConfigItemDefinition.CHANNELS_AI_TAGGED);
    }

    public List<String> allowedDms() {
        return getList(ConfigItemDefinition.ALLOWED_DMS);
    }

    public String blockUser() {
        return getString(ConfigItemDefinition.BLOCK_USER);
    }

    public String allowUser() {
        return getString(ConfigItemDefinition.ALLOW_USER);
    }

    public String blockPath() {
        return getString(ConfigItemDefinition.BLOCK_PATH);
    }

    public String allowPath() {
        return getString(ConfigItemDefinition.ALLOW_PATH);
    }

    public String blockChannel() {
        return getString(ConfigItemDefinition.BLOCK_CHANNEL);
    }

    public String allowChannel() {
        return getString(ConfigItemDefinition.ALLOW_CHANNEL);
    }

    // ── Channel capabilities (moved from NcbotProperties) ──────────────

    /**
     * Resolve channel capabilities for a given request.
     * DMs bypass channel lists and use default capabilities if the sender key is allowed.
     * Non-DMs resolve from flat property lists.
     */
    public Optional<ChannelCapabilities> getChannelCapabilities(ChatRequest request) {
        if (request.isDm()) {
            log.debug("getChannelCapabilities: DM from {}", request.senderKey());
            if (!allowedDms().isEmpty() && !allowedDms().contains(request.senderKey())) {
                log.debug("getChannelCapabilities: DM sender key {} not in allowed list", request.senderKey());
                return Optional.empty();
            }
            return Optional.of(DM_CAPABILITIES);
        }
        return Optional.of(getChannelCapabilities(request.channelName()));
    }

    public ChannelCapabilities getChannelCapabilities(String channelName) {
        return channelCapabilities.computeIfAbsent(channelName, this::from);
    }

    /**
     * Check whether command handling is enabled for the given request.
     */
    public boolean isCommandEnabled(ChatRequest request) {
        return getChannelCapabilities(request)
                .map(ChannelCapabilities::command)
                .orElse(false);
    }

    private ChannelCapabilities from(String channelName) {
        ChannelCapabilities result = ChannelCapabilities.builder()
                .welcome(PatternUtil.matches(channelName, welcomeChannelPattern()))
                .command(PatternUtil.matches(channelName, commandChannelPattern()))
                .pathUpgrade(PatternUtil.matches(channelName, pathUpgradeChannelPattern()))
                .ai(resolveAiMode(channelName))
                .build();
        log.debug("channel capabilities for {}: {}", channelName, result);
        return result;
    }

    private AiMode resolveAiMode(String channelName) {
        if (!aiEnabled()) {
            return AiMode.DISABLED;
        }
        if (PatternUtil.matches(channelName, aiEachChannelPattern())) {
            return AiMode.EACH;
        }
        if (PatternUtil.matches(channelName, aiTaggedChannelPattern())) {
            return AiMode.TAGGED;
        }
        return AiMode.DISABLED;
    }

    // ── Verification ───────────────────────────────────────────────────

    private String validate(ConfigItemDefinition def, String rawValue) {
        String value = rawValue == null ? "" : rawValue;
        switch (def.type()) {
            case BOOLEAN -> {
                if (!value.trim().equalsIgnoreCase("true") && !value.trim().equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException(def.key() + " must be true or false");
                }
            }
            case INT -> {
                try {
                    Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(def.key() + " must be an integer");
                }
            }
            case LONG -> {
                try {
                    Long.parseLong(value.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(def.key() + " must be an integer");
                }
            }
            case STRING, TEXT, LIST -> {
                // Accept any string; LIST values are split on commas when read.
            }
        }
        // Store trimmed values for scalar types but preserve whitespace for free text.
        return switch (def.type()) {
            case TEXT -> value;
            default -> value.strip();
        };
    }

    /** A definition paired with its current effective value for the admin UI. */
    public record ConfigItemEntry(ConfigItemDefinition definition, String value, boolean isDefault) {

        public String key() {
            return definition.key();
        }

        public ConfigType type() {
            return definition.type();
        }

        public String description() {
            return definition.description();
        }

        public String defaultValue() {
            return definition.defaultValue();
        }

        public boolean restartRequired() {
            return definition.restartRequired();
        }
    }

}
