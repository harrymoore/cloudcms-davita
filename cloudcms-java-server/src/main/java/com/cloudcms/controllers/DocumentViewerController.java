/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import java.util.List;

import com.cloudcms.server.CloudCmsDriverBranchNotFoundException;
import com.cloudcms.server.CloudcmsDriver;

import org.gitana.platform.client.node.Node;
import org.gitana.platform.client.node.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DocumentViewerController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CloudcmsDriver driver;

    private static final String VIEW_NAME = "index";
    private static final String NODE_TYPE = "davita:document";

    @GetMapping(value = { "", "/", "/index", "index/{id}", "index.html/{id}" })
    // @Cacheable(value = "page-cache", condition = "null == #cacheResults ? true :
    // #cacheResults.equals('true')", key = "#branch + '-' + #id + '-' + #metadata")
    public String getDocument(@PathVariable(required = false) final String id,
            @RequestParam(required = false) final String branch, 
            @RequestParam(required = false) final String metadata,
            @RequestParam(required = false, defaultValue = "all") final String filter,
            @RequestParam(required = false, defaultValue = "true") final String useCache, 
            final Model map)
            throws CloudCmsDriverBranchNotFoundException {

        // retrieve only metadata. not a binary attachment
        Boolean includeMetadata = Boolean.parseBoolean(metadata);

        log.debug("/index/{} {}", id, map.toString());

        map.addAttribute("filter", filter);

        // add the list of documents to the model so that an index can be built
        List<Node> nodes = driver.queryNodesByType(driver.getBranch(branch).getId(), driver.getLocale(), NODE_TYPE, Boolean.parseBoolean(useCache));
        map.addAttribute("documents", nodes);

        // add the requested document, if it exists, to the model
        if (null != id && !id.isEmpty()) {
            map.addAttribute("document", this.driver.getNodeById(driver.getBranch(branch).getId(), driver.getLocale(), id, Boolean.parseBoolean(useCache)));
            // log.debug("{}", ((NodeImpl)map.getAttribute("document")).get("title"));
            // log.debug("{}", ((NodeImpl)map.getAttribute("document")).getSystemObject().get("modified_on").get("iso_8601").textValue());
            // log.debug("{}", ((NodeImpl)map.getAttribute("document")).getSystemObject().get("modified_by").textValue());
        }
        else
        {
            map.addAttribute("document", nodes.size() > 0 ? nodes.get(0) : null);
            // map.addAttribute("document", null);
        }

        return VIEW_NAME;
    }
}