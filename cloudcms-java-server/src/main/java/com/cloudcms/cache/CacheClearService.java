/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.cloudcms.server.CloudCmsDriverBranchNotFoundException;
import com.cloudcms.server.CloudcmsDriver;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.gitana.platform.services.association.Direction;
import org.gitana.platform.support.QName;
import org.gitana.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Configuration
@EnableScheduling
@CacheConfig
public class CacheClearService {
    private final Logger log = LoggerFactory.getLogger(CacheClearService.class);
    private final ConcurrentHashMap<String, List<String>> invalideNodes = new ConcurrentHashMap<>();

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private CloudcmsDriver driver;

    @Autowired
    private KeyGenerator nodeCacheKeyGenerator;

    /**
     * Empty the node cache
     */
    @CacheEvict(value = "node-cache", allEntries = true)
    public void clearNodeCache() {
        log.info("Empty node cache");
    }

    /**
     * Empty the node attachmentcache
     */
    @CacheEvict(value = "attachment-cache", allEntries = true)
    public void clearAttachmetCache() {
        log.info("Empty node attachment cache");
    }

    /**
     * Clear all caches
     */
    public void clearCache() {
        log.info("Clearing cache");
        cacheManager.getCache("node-cache").clear();
        cacheManager.getCache("attachment-cache").clear();
        cacheManager.getCache("query-cache").clear();
        // cacheManager.getCache("page-cache").clear();
    }


    /**
     * called to clear a list of nodes from the node-cache. usually called by a
     * JMS/SQS message event handler after recieving and invalidation notification
     * 
     * add the invalid node Ids to a datastructure that will be read in another
     * thread to clear the cache
     * 
     * @param nodeRefs
     */
    public void clearCacheKeysFromNodeRefs(final List<String> nodeRefs) {
        log.trace(String.format("Clear node(s) from cache %s", nodeRefs));

        nodeRefs.forEach(ref -> {
            String[] parts = ref.substring(7).split("/");
            final String branchId = parts[2];
            final String nodeId = parts[3];
            invalideNodes.computeIfAbsent(branchId, k -> new ArrayList<>());
            synchronized (invalideNodes) {
                if (!invalideNodes.get(branchId).contains(nodeId)) {
                    log.trace(String.format("adding node id %s to queue for invalidation on branch %s", nodeId,
                            branchId));
                    invalideNodes.get(branchId).add(nodeId);
                }
            }
        });
    }

    /**
     * clear individual node cache entries periodically. invalidNodes is populated
     * when invalidation notifications are received via SQS messages. This method
     * processses all the entries in invalidNodes and then empties it.
     * 
     */
    @Scheduled(fixedRate = 10000)
    public void clearCacheEntries() {
        log.debug("checking cache invalidation queue");
        invalideNodes.forEach((branchId, nodeList) -> {
            if (!nodeList.isEmpty()) {
                ObjectNode query = JsonUtil.createObject();
                query.set("_doc", JsonUtil.createObject().set("$in", JsonUtil.createArray(nodeList)));

                try {
                    driver.queryNodes(branchId, query, null, CloudcmsDriver.IGNORE_CACHE).forEach(node -> {
                        log.debug(String.format("node from invalidate query %s", node.getId()));

                        // remove all locales for this node from the cache
                        node.associations(QName.create("a:has_translation"), Direction.OUTGOING)
                                .forEach((associationId, associationNode) -> {
                                    log.trace(String.format("association %s from %s", associationId,
                                            associationNode.toString()));
                                    clearCachedNode(branchId, associationNode.get("locale").toString(), node.getId(),
                                            null);
                                });

                        // remove all attachments for this node from the cache
                        node.listAttachments().forEach((attachmentId, attachmentObject) -> {
                            log.trace("attachment {} {}", attachmentId, attachmentObject);
                            clearCachedNode(branchId, null, node.getId(), attachmentId);
                        });

                        // remove the default locale and branch
                        clearCachedNode(branchId, null, node.getId(), null);
                    });

                } catch (CloudCmsDriverBranchNotFoundException e) {
                    log.warn(e.getMessage());
                }
            }

            nodeList.clear();
        });
    }

    /**
     * evict cache entry by branch, locale and nodeid (or path)
     * 
     * @param branch
     * @param locale
     * @param nodeIdorPath
     * @param nodeId
     * @param attachmentId
     */
    private void clearCachedNode(final String branch, final String locale, final String nodeIdorPath,
            final String attachmentId) {
        String key = ((NodeCacheKeyGenerator) nodeCacheKeyGenerator).getKey(branch, locale, nodeIdorPath, attachmentId);
        log.info(String.format("Evicting node from cache with key %s", key));
        if (attachmentId == null || attachmentId.equals("")) {
            cacheManager.getCache("node-cache").evictIfPresent(key);

        } else {
            cacheManager.getCache("attachment-cache").evictIfPresent(key);
        }
    }
}
