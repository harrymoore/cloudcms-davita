/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.cache;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Component;

@Component("nodeCacheKeyGenerator")
public class NodeCacheKeyGenerator extends SimpleKeyGenerator {
    private final Logger log = LoggerFactory.getLogger(NodeCacheKeyGenerator.class);

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return getKey((String) params[0], (String) params[1], (String) params[2]);
    }

    public String getKey(String branch, String locale, String nodeId) {
        return getKey(branch, locale, nodeId, null);
    }

    public String getKey(String branch, String locale, String nodeId, String attachmentId) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(branch);
        if (locale != null && !locale.equals("")) {
            keyBuilder.append("-");
            keyBuilder.append(locale);
        }
        keyBuilder.append("-");
        keyBuilder.append(nodeId);
        if (attachmentId != null && !attachmentId.equals("")) {
            keyBuilder.append("-");
            keyBuilder.append(attachmentId);
        }

        String key = keyBuilder.toString();
        log.debug("Key {}", key);
        return keyBuilder.toString();
    }
}
