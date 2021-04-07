/**
 * Copyright (C) 2021 Gitana Software, Inc.
 */
package com.cloudcms.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

@Service
@Configuration
@EnableScheduling
@CacheConfig
public class CacheClearService {
    private final Logger log = LoggerFactory.getLogger(CacheClearService.class);

    @Autowired
    CacheManager cacheManager;

    /**
     * Clear all caches
     */
    @NonNull
    public void clearCache() {
        log.info("Clearing cache");
        cacheManager.getCache("node-cache").clear();
        cacheManager.getCache("attachment-cache").clear();
        cacheManager.getCache("query-cache").clear();
    }
}
