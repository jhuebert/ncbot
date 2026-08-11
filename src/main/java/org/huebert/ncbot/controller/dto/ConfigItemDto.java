package org.huebert.ncbot.controller.dto;

import org.huebert.ncbot.config.ConfigType;
import org.huebert.ncbot.service.ConfigService;

/**
 * A configuration item as exposed to the admin UI: the code-defined metadata
 * (key, type, description, default, restart flag) plus the current effective value.
 */
public record ConfigItemDto(
        String key,
        ConfigType type,
        String value,
        String defaultValue,
        String description,
        boolean restartRequired,
        boolean isDefault
) {

    public static ConfigItemDto from(ConfigService.ConfigItemEntry entry) {
        return new ConfigItemDto(
                entry.key(),
                entry.type(),
                entry.value(),
                entry.defaultValue(),
                entry.description(),
                entry.restartRequired(),
                entry.isDefault()
        );
    }
}
