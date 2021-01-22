/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import java.util.List;
import java.util.Map;

import com.cloudcms.server.CloudCmsDriverBranchNotFoundException;
import com.cloudcms.server.CloudcmsDriver;

import org.gitana.platform.client.attachment.Attachment;
import org.gitana.platform.client.node.Node;
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

        log.debug("getDocument()");
        
        // retrieve only metadata. not a binary attachment
        // Boolean includeMetadata = Boolean.parseBoolean(metadata);

        map.addAttribute("filter", filter);

        final Boolean cache = Boolean.parseBoolean(useCache);

        // add the list of documents to the model so that an index can be built
        List<Node> nodes = driver.queryNodesByType(driver.getBranch(branch).getId(), driver.getLocale(), NODE_TYPE, cache);
        map.addAttribute("indexDocuments", nodes);

        // add the requested document, if it exists, to the model
        if (null != id && !id.isEmpty()) {
            Node node = driver.getNodeById(driver.getBranch(branch).getId(), driver.getLocale(), id, cache);
            map.addAttribute("document", node);

            // for each "document" relator item, gather info about the related node and it's "default" attachment
            List<Map<String,String>> relatedDocuments = (List<Map<String,String>>)node.get("document");
            for(Map<String,String> doc : relatedDocuments) {
                log.debug("{} {}", doc.get("id"), doc.get("ref"));
                Attachment attachment = driver.getNodeAttachmentById(driver.getBranch(branch).getId(), driver.getLocale(), (String)doc.get("id"), "default", cache);

                doc.put("mimetype", attachment.getContentType());
                doc.put("isVideo", String.valueOf(attachment.getContentType().startsWith("video/")));
                doc.put("isAudio", String.valueOf(attachment.getContentType().startsWith("audio/")));
                doc.put("isPdf", String.valueOf(attachment.getContentType().endsWith("/pdf")));
            }

            map.addAttribute("attachments", relatedDocuments);

            // node.associations(QName.create("a:linked"), Direction.OUTGOING).asList().forEach(association -> {
            //     map.addAttribute(association.get, attributeValue)
            // });;
            // log.debug("{}", ((NodeImpl)map.getAttribute("document")).get("title"));
            // log.debug("{}", ((NodeImpl)map.getAttribute("document")).getSystemObject().get("modified_on").get("iso_8601").textValue());
            // log.debug("{}", ((NodeImpl)map.getAttribute("document")).getSystemObject().get("modified_by").textValue());

        }
        else
        {
            map.addAttribute("document", !nodes.isEmpty() ? nodes.get(0) : null);
        }

        return VIEW_NAME;
    }
}