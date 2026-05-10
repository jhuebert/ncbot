package org.huebert.ncbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.huebert.ncbot.config.NcbotProperties;

@SpringBootApplication
@EnableConfigurationProperties(NcbotProperties.class)
public class NcbotApplication {

    static void main(String[] args) {
        SpringApplication.run(NcbotApplication.class, args);
    }

}
