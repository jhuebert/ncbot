package org.huebert.ncbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.controller.dto.ConfigItemDto;
import org.huebert.ncbot.controller.dto.ConfigItemUpdateRequest;
import org.huebert.ncbot.service.ConfigService;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for managing DB-backed configuration items. Reads expose every
 * known item (with type, description and default); writes validate the value
 * against its declared type and take effect immediately for runtime items.
 */
@Slf4j
@RestController
@RequestMapping("/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @DebugLog
    @GetMapping
    public List<ConfigItemDto> list() {
        return configService.list().stream().map(ConfigItemDto::from).toList();
    }

    @DebugLog
    @PutMapping("/{key}")
    public ConfigItemDto update(@PathVariable String key, @RequestBody ConfigItemUpdateRequest request) {
        return ConfigItemDto.from(configService.update(key, request.value()));
    }

    /** Restore a configuration item to its seeded default value. */
    @DebugLog
    @DeleteMapping("/{key}")
    public ConfigItemDto reset(@PathVariable String key) {
        return ConfigItemDto.from(configService.reset(key));
    }

}
