package com.resumeai.resumeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AppConfig provides infrastructure beans used across the service.
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate is used for simple synchronous HTTP calls
     * from resume-service to section-service.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
