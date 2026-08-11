package org.huebert.ncbot;

import com.openai.core.Timeout;
import org.huebert.ncbot.config.NcbotProperties;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy
@EnableConfigurationProperties(NcbotProperties.class)
public class NcbotApplication {

    static void main(String[] args) {
        SpringApplication.run(NcbotApplication.class, args);
    }

    /**
     * Apply a uniform timeout to the OpenAI-compatible AI client.
     *
     * Spring AI 2.x drives the model through the official OpenAI Java SDK
     * (OkHttp), so the AI call's timeouts are controlled here rather than via
     * a Spring RestClient. The value comes from {@code ncbot.ai-timeout}
     * (env {@code NCBOT_AI_TIMEOUT}, default 10m).
     *
     * {@code SpringAiOpenAiHttpClient.Builder#timeout(Duration)} only sets the
     * top-level *request* (OkHttp call) timeout; connect/read/write stay at the
     * SDK default of 60s. A slow local model that streams nothing for >60s would
     * therefore be cut off regardless of {@code ai-timeout}. Set every timeout so
     * the configured value actually takes effect.
     */
    @Bean
    public OpenAiHttpClientBuilderCustomizer aiHttpClientTimeoutCustomizer(NcbotProperties properties) {
        java.time.Duration timeout = properties.aiTimeout();
        return builder -> builder.timeout(Timeout.builder()
                .connect(timeout)
                .read(timeout)
                .write(timeout)
                .request(timeout)
                .build());
    }

}
