/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.cloudcms.server.CloudcmsDriver;
import com.cloudcms.server.CmsDriverBranchNotFoundException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.gitana.platform.client.attachment.Attachment;
import org.gitana.platform.client.node.Node;
import org.gitana.platform.support.Pagination;
import org.gitana.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DocumentViewerController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.ui-template}")
    private String template;

    @Autowired
    private CloudcmsDriver driver;

    private static final String NODE_TYPE = "davita:document";

    @GetMapping(value = { "/" })
    public String redirectToRoot() {
        return "redirect:/documents";
    }

    @GetMapping(value = { "/logout" })
    public String logout(HttpServletRequest request) throws ServletException {
        request.logout();
        return "redirect:/documents";
    }

    @GetMapping(value = { "/documents", "/documents/{nodeId}" })
    public String getDocument(@PathVariable(required = false) String nodeId,
            @RequestParam(required = false) final String branchId, 
            @RequestParam(required = false) final String metadata,
            @RequestParam(required = false) final String rangeFilter,
            @RequestParam(required = false, defaultValue = "") final String tagFilter,
            @RequestParam(required = false, defaultValue = "true") final String useCache, 
            final Model map)
            throws CmsDriverBranchNotFoundException {

        log.debug("getDocument()");
        
        // retrieve only metadata. not a binary attachment
        // Boolean includeMetadata = Boolean.parseBoolean(metadata);

        map.addAttribute("rangeFilter", rangeFilter == null ? "" : rangeFilter);
        map.addAttribute("tagFilter", tagFilter == null ? "" : tagFilter);

        final Boolean cache = Boolean.parseBoolean(useCache);

        // add the list of documents to the model so that an index can be built
        List<Node> indexNodes = driver.queryNodesByType(driver.getBranch(branchId).getId(), rangeFilter, tagFilter, NODE_TYPE, cache);
        map.addAttribute("indexDocuments", indexNodes);

        // if ((nodeId == null || nodeId.isEmpty()) && !indexNodes.isEmpty()) {
        //     return String.format("redirect:/documents/%s", indexNodes.get(0).getId());
        // }

        Boolean hasVideo = false;
        Boolean hasAudio = false;
        Boolean hasPdf = false;
        Boolean hasImage = false;

        // add the requested document, if it exists, to the model
        if (null != nodeId && !nodeId.isEmpty()) {
            Node node = driver.getNodeById(driver.getBranch(branchId).getId(), nodeId, cache);
            map.addAttribute("document", node);

            // for each "document" relator item, gather info about the related node and it's "default" attachment
            @SuppressWarnings("unchecked")
            List<Map<String,String>> relatedDocuments = (List<Map<String,String>>)node.get("document");
            for(Map<String,String> doc : relatedDocuments) {
                Attachment attachment = driver.getNodeAttachmentById(driver.getBranch(branchId).getId(), (String)doc.get("id"), "default", cache);

                boolean isOther = true;

                doc.put("id", doc.get("id"));
                doc.put("attachmentId", attachment.getId());
                doc.put("mimetype", attachment.getContentType());
                doc.put("isVideo", String.valueOf(attachment.getContentType().startsWith("video/") 
                    || attachment.getContentType().endsWith("/ogg") 
                    || attachment.getContentType().endsWith("/ogv")));
                doc.put("isAudio", String.valueOf(attachment.getContentType().startsWith("audio/")));
                doc.put("isPdf", String.valueOf(attachment.getContentType().endsWith("/pdf")));
                doc.put("isImage", String.valueOf(attachment.getContentType().startsWith("image/")));

                if (Boolean.parseBoolean(doc.get("isVideo"))) {
                    hasVideo = true;
                    isOther = false;
                }
                if (Boolean.parseBoolean(doc.get("isAudio"))) {
                    hasAudio = true;
                    isOther = false;
                }
                if (Boolean.parseBoolean(doc.get("isPdf"))) {
                    hasPdf = true;
                    isOther = false;
                }
                if (Boolean.parseBoolean(doc.get("isImage"))) {
                    hasImage = true;
                    isOther = false;
                }                

                doc.put("isOther", String.valueOf(isOther));
            }

            map.addAttribute("attachments", relatedDocuments);
            
            map.addAttribute("hasVideo", hasVideo);
            map.addAttribute("hasAudio", hasAudio);
            map.addAttribute("hasPdf", hasPdf);
            map.addAttribute("hasImage", hasImage);
        }
        else
        {
            map.addAttribute("hasVideo", false);
            map.addAttribute("hasAudio", false);
            map.addAttribute("hasPdf", false);
            map.addAttribute("hasImage", false);
        }

        ObjectNode query = JsonUtil.createObject();
        query.put("_type", "n:tag");
        query.set("_fields", JsonUtil.createObject().put("title", 1).put("tag", 1).put("_type", 1).put("_qname", 1));

        Pagination pagination = new Pagination();
        pagination.setLimit(1000);
        pagination.getSorting().addSort("title", 1);

        map.addAttribute("tags", driver.queryNodes(driver.getBranch(branchId).getId(), query, pagination, cache));

        return template;
    }
}