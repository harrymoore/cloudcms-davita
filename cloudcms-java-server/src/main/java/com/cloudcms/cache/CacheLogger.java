/**
 * Copyright (C) 2021 Gitana Software, Inc.
 */
package com.cloudcms.cache;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheLogger implements CacheEventListener<Object, Object> {
  private final Logger log = LoggerFactory.getLogger(CacheLogger.class);

  @Override
  public void onEvent(CacheEvent<?, ?> cacheEvent) {
    log.debug("{} {}", cacheEvent.getType(), cacheEvent.getKey());
  }
}