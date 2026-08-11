package org.huebert.ncbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The small set of configuration values that are consumed only at bean-construction /
 * startup time and therefore cannot live in the database-managed runtime configuration:
 * <ul>
 *   <li>{@code ai-timeout} — baked into the OpenAI HTTP client when it is built.</li>
 *   <li>{@code memory-update-period} — drives the fixed-delay scheduler annotation.</li>
 *   <li>{@code weather-timeout} — baked into the weather {@code RestClient} when it is built.</li>
 * </ul>
 * <p>
 * Everything user-editable at runtime lives in the {@code config_item} table and is
 * read through {@link org.huebert.ncbot.service.ConfigService}. Spring AI credentials,
 * the model name, and other infrastructure (server, datasource) stay in
 * {@code application.yml} as well.
 */
@ConfigurationProperties(prefix = "ncbot")
public record NcbotProperties(
        Duration aiTimeout,
        Duration memoryUpdatePeriod,
        Duration weatherTimeout
) {
}
