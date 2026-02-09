package com.srishna.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables caching for post list APIs. Cache type and spec are configured in application.yml
 * (spring.cache.type=caffeine, spring.cache.caffeine.spec). Eviction on write is in PostService.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
