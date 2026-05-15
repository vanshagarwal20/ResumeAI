package com.resumeai.template.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * RestTemplate is used to call resume-service
     * when template usage statistics are needed.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

