/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import java.util.List;

import com.cloudcms.server.CloudCmsDriverBranchNotFoundException;
import com.cloudcms.server.CloudcmsDriver;
import org.gitana.platform.client.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CloudcmsDriver driver;

    private static final String VIEW_NAME = "index";
    private static final String NODE_TYPE = "davita:document";

    @GetMapping(value = { "", "/", "index", "index.html" })
    public String getDocument(@RequestParam(required = false) final String branch,
            @RequestParam(required = false) final String locale, final Model map) throws CloudCmsDriverBranchNotFoundException {

        log.debug(String.format("%s %s", branch, locale));

        List<Node> docs = driver.queryNodesByType(driver.getBranch(branch).getId(), driver.getLocale(locale), NODE_TYPE, CloudcmsDriver.USE_CACHE);
        map.addAttribute("documents", docs);

        return VIEW_NAME;
    }
}