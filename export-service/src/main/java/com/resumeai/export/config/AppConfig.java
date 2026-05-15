package com.resumeai.export.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * RestTemplate is used to call other microservices:
     * - resume-service
     * - section-service
     * - template-service
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
