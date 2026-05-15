package com.resumeai.auth.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AppConfig — General application beans that don't fit in other config classes.
 */
@Configuration
public class AppConfig {

    /**
     * ModelMapper — automatically maps fields between Entity and DTO classes.
     *
     * STRICT matching means field names must match exactly.
     * This prevents accidental mappings between similarly-named but unrelated fields.
     *
     * Usage:
     *   UserProfileResponse dto = modelMapper.map(userEntity, UserProfileResponse.class);
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // STRICT: only map fields with exact name AND type match
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }
}
