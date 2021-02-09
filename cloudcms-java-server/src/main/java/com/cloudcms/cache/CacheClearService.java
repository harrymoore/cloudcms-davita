/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.cache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

@Service
@Configuration
@EnableScheduling
@CacheConfig
public class CacheClearService {
    private final Logger log = LoggerFactory.getLogger(CacheClearService.class);
    // private final ConcurrentHashMap<String, List<String>> invalideNodes = new ConcurrentHashMap<>();

    @Autowired
    CacheManager cacheManager;

    /**
     * Clear all caches
     */
    public void clearCache() {
        log.info("Clearing cache");
        cacheManager.getCache("node-cache").clear();
        cacheManager.getCache("attachment-cache").clear();
        cacheManager.getCache("query-cache").clear();
    }
}
