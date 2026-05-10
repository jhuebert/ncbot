package org.huebert.ncbot;

import org.huebert.ncbot.config.NcbotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NcbotProperties.class)
public class NcbotApplication {

    static void main(String[] args) {
        SpringApplication.run(NcbotApplication.class, args);
    }

}
