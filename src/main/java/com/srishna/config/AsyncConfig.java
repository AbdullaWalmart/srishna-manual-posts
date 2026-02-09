package com.srishna.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /** Bounded executor for GCS DB sync so API responses return immediately. */
    @Bean(name = "dbSyncExecutor")
    public Executor dbSyncExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(1);
        e.setMaxPoolSize(2);
        e.setQueueCapacity(10);
        e.setThreadNamePrefix("db-sync-");
        e.initialize();
        return e;
    }

    /** Single-thread executor for image URL cache warming (runs after startup, does not block requests). */
    @Bean(name = "cacheWarmerExecutor")
    public Executor cacheWarmerExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(1);
        e.setMaxPoolSize(1);
        e.setQueueCapacity(1);
        e.setThreadNamePrefix("url-cache-warmer-");
        e.initialize();
        return e;
    }
}
