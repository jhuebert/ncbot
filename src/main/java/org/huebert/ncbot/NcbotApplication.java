package org.huebert.ncbot;

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
     * Apply a uniform request timeout to the OpenAI-compatible AI client.
     *
     * Spring AI 2.x drives the model through the official OpenAI Java SDK
     * (OkHttp), so the AI call's timeout is controlled here rather than via
     * a Spring RestClient. The value comes from {@code ncbot.ai-timeout}
     * (env {@code NCBOT_AI_TIMEOUT}, default 10m).
     */
    @Bean
    public OpenAiHttpClientBuilderCustomizer aiHttpClientTimeoutCustomizer(NcbotProperties properties) {
        return builder -> builder.timeout(properties.aiTimeout());
    }

}
