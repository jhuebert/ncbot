package org.huebert.ncbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A single, named configuration value persisted in the database.
 * <p>
 * Keys use dot-separated names (e.g. {@code bot.name}, {@code chat.max-reply-bytes})
 * as defined by the {@link org.huebert.ncbot.config.ConfigItemDefinition} registry.
 * Values are stored as raw strings and typed on read/validation by the registry's
 * {@code ConfigType}.
 * <p>
 * Every definition is seeded with a sensible default on first start, so the table is
 * never empty and every configuration item always resolves to a usable value.
 */
@Builder
@Data
@Entity
@Table(name = "config_item")
@NoArgsConstructor
@AllArgsConstructor
public class ConfigItem {

    /** Dot-separated configuration key, e.g. {@code bot.system-prompt}. */
    @Id
    @Column(name = "config_key", nullable = false, length = 255)
    private String key;

    /** Raw string value. Interpreted according to the definition's type. */
    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    /** When this value was last written (null for the freshly-seeded default). */
    @Column(name = "updated_at")
    private Instant updatedAt;

}
