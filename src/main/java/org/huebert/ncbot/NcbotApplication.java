package org.huebert.ncbot;

import org.huebert.ncbot.config.NcbotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy
@EnableConfigurationProperties(NcbotProperties.class)
public class NcbotApplication {

    static void main(String[] args) {
        SpringApplication.run(NcbotApplication.class, args);
    }

    @Bean
    public RestClientCustomizer restClientTimeoutCustomizer() {
        return restClientBuilder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(600000);
            factory.setReadTimeout(600000);
            restClientBuilder.requestFactory(factory);
        };
    }

}
