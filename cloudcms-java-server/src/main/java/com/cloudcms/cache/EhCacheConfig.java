/**
 * Copyright (C) 2021 Gitana Software, Inc.
 */
package com.cloudcms.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class EhCacheConfig {   
    // @Bean("nodeCacheKeyGenerator")
    // public KeyGenerator keyGenerator() {
    //     return new nodeCacheKeyGenerator();
    // }

    @Bean
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }
}