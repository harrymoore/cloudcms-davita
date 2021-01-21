/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.cloudcms.cache.CacheClearService;
import com.cloudcms.server.CloudcmsDriver;

import org.gitana.platform.client.branch.Branch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CloudcmsDriver driver;

    @Autowired
    private CacheClearService cacheClearService;


    @GetMapping(value = "/clearcache")
    public @ResponseBody Map<String, String> clearcache() {
        log.trace("clearcache");

        cacheClearService.clearCache();

        Map<String, String> map = new HashMap<>();
        map.put("status", "success");

        return map;
    }

    @GetMapping(value = "/health")
    public @ResponseBody Map<String, String> healthcheck1() {
        log.trace("health");

        Map<String, String> map = new HashMap<>();
        map.put("health", "ok");

        return map;
    }

    @GetMapping(value = "/healthcheck")
    public @ResponseBody Map<String, String> healthcheck2() {
        log.trace("healthcheck");

        Map<String, String> map = new HashMap<>();
        map.put("health", "ok");

        return map;
    }

    static final Long MB = 1024l*1024l;

    @GetMapping(value = "/ping")
    public @ResponseBody Map<String, String> healthcheck3() {
        log.trace("ping");

        Branch branch = driver.getBranch();

        Map<String, String> map = new TreeMap<>();
        map.put("repository", driver.getContentRepository().getId());
        map.put("branchId", branch.getId());
        map.put("branchTitle", branch.getTitle());

		Runtime runtime = Runtime.getRuntime();
        map.put("freeMemoryMB", Long.toString(runtime.freeMemory() / MB));
        map.put("totalMemoryMB", Long.toString(runtime.totalMemory() / MB));
        map.put("maxMemoryMB", Long.toString(runtime.maxMemory() / MB));

        return map;
    }
}