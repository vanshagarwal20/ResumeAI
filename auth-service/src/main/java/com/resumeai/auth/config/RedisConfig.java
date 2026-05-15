package com.resumeai.auth.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * RedisConfig — Sets up the Redis connection and cache manager for auth-service.
 *
 * @EnableCaching activates @Cacheable / @CacheEvict annotations across the service.
 */
@Slf4j
@EnableCaching
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    // ── 1. Connection Factory ─────────────────────────────────────────────────
    // Lettuce is the NIO-based Redis client included with Spring Data Redis.
    // We configure a connection pool so multiple threads can share connections.

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(host);
        serverConfig.setPort(port);
        serverConfig.setDatabase(database);

        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }

        // Pool settings — critical for multi-threaded Spring apps
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);       // max simultaneous Redis connections
        poolConfig.setMaxIdle(10);        // max idle connections kept alive
        poolConfig.setMinIdle(2);         // always keep 2 connections ready
        poolConfig.setTestOnBorrow(true); // check connection health before use

        LettucePoolingClientConfiguration clientConfig =
            LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .poolConfig(poolConfig)
                .build();

        log.info("Connecting to Redis at {}:{} (DB {})", host, port, database);
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    // ── 2. RedisTemplate ─────────────────────────────────────────────────────
    // Used for manual Redis operations: SET, GET, DEL, EXPIRE, INCR, etc.
    // Keys are plain strings. Values are stored as JSON with type metadata.

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Keys: plain UTF-8 strings (readable in redis-cli)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Values: JSON (includes Java class type so deserialization is safe)
        GenericJackson2JsonRedisSerializer jsonSerializer = jsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // ── 3. CacheManager ──────────────────────────────────────────────────────
    // Powers @Cacheable, @CacheEvict, @CachePut annotations.
    // Each cache name gets its own TTL.

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = jsonSerializer();

        // Default: 10 minutes TTL for caches not listed below
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues(); // never cache null — causes subtle bugs

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // User profiles — fetched on every page load; 15 min is safe
        cacheConfigs.put("userProfile",
            defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Blacklisted tokens — must survive until access token expires (24h in your yml)
        cacheConfigs.put("blacklistedTokens",
            defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper om = new ObjectMapper();
        // Store Java class name in JSON so deserializer knows the right type
        om.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(om);
    }
}