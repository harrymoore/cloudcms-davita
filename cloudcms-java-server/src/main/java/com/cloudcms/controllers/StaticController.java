/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import com.cloudcms.server.CmsDriverBranchNotFoundException;
import com.cloudcms.server.CloudcmsDriver;

import org.gitana.platform.client.attachment.Attachment;
import org.gitana.platform.client.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ContentDisposition.Builder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class StaticController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CloudcmsDriver driver;

    /**
     * Return the default attachment (if found) of the node identified by id
     * 
     * @param id
     * @param branchId
     * @param locale
     * @param attachmentId Optional id of the attachment. default is "default"
     * @param metadata
     * @return
     */
    @GetMapping(value = "/attachment/{nodeId}")
    public @ResponseBody ResponseEntity<byte[]> staticById(@PathVariable final String nodeId,
            @RequestParam(required = false) final String branchId, 
            @RequestParam(required = false) final String attachmentId,
            @RequestParam(required = false) final String name,
            @RequestParam(required = false, defaultValue = "inline") final String disposition,
            @RequestAttribute(required = false) final Boolean metadata) 
                throws CmsDriverBranchNotFoundException {
        
        final Node node = driver.getNodeById(driver.getBranch(branchId).getId(), nodeId, CloudcmsDriver.USE_CACHE);
        final Attachment attachment = node.listAttachments().get(attachmentId == null ? "default" : attachmentId);
        final Builder dispositionBuilder = disposition.equalsIgnoreCase("inline") ? ContentDisposition.inline() : ContentDisposition.attachment();
        final String fileName = (name != null ? name : node.getTitle());

        return ResponseEntity.ok()
            .header("Content-Disposition", dispositionBuilder.filename(fileName).build().toString())
            .contentLength(attachment.getLength())
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .body(node.downloadAttachment(attachmentId == null ? "default" : attachmentId));        
    }

    /**
     * Return a preview of the requested node's attachment
     * 
     * @param id
     * @param branch
     * @param locale
     * @param attachmentId
     * @param name
     * @param metadata
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/preview/{nodeId}")
    public @ResponseBody ResponseEntity<byte[]> previewById(@PathVariable final String nodeId,
            @RequestParam(required = false) final String branchId,
            @RequestParam(required = true) final String attachmentId,
            @RequestParam(required = false, defaultValue = "") final String name,
            @RequestParam(required = true) final String mimetype, @RequestParam(required = true) final String size,
            @RequestParam(required = false, defaultValue = "inline") final String disposition,
            @RequestAttribute(required = false) final Boolean metadata) 
                throws Exception {
        
        final Node node = driver.getNodeById(driver.getBranch(branchId).getId(), nodeId, CloudcmsDriver.USE_CACHE);
        final Builder dispositionBuilder = disposition.equalsIgnoreCase("inline") ? ContentDisposition.inline() : ContentDisposition.attachment();
        final String fileName = (!name.isEmpty() ? name : node.getTitle());
        final byte[] bytes = driver.getDocumentPreviewBytesById(driver.getBranch(branchId).getId(), nodeId, attachmentId, mimetype, size, CloudcmsDriver.USE_CACHE);

        return ResponseEntity.ok()
            .header("Content-Disposition", dispositionBuilder.filename(fileName).build().toString())
            .contentLength(bytes.length)
            .contentType(MediaType.parseMediaType(mimetype))
            .body(bytes);        
    }
}