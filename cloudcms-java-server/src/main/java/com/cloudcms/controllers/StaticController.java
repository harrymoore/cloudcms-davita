/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import com.cloudcms.server.CloudCmsDriverBranchNotFoundException;
import com.cloudcms.server.CloudcmsDriver;

import org.gitana.platform.client.attachment.Attachment;
import org.gitana.platform.client.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CloudcmsDriver driver;

    /**
     * Return the default attachment (if found) of the node identified by id
     * 
     * @param id
     * @param branch
     * @param locale
     * @param attachmentName Optional id of the attachment. default is "default"
     * @param metadata
     * @return
     */
    @GetMapping(value = "/attachment/{id}")
    public @ResponseBody ResponseEntity<byte[]> staticById(@PathVariable final String id,
            @RequestParam(required = false) final String branch, 
            @RequestParam(required = false) final String locale,
            @RequestParam(required = false) final String attachmentName,
            @RequestParam(required = false) final String name,
            @RequestAttribute(required = false) final Boolean metadata) throws CloudCmsDriverBranchNotFoundException {
        
        log.debug("{} {} {}", branch, locale, id);

        final Node node = driver.getNodeById(driver.getBranch(branch).getId(), driver.getLocale(locale), id, CloudcmsDriver.USE_CACHE);
        final Attachment attachment = node.listAttachments().get(attachmentName == null ? "default" : attachmentName);

        return ResponseEntity.ok()
            .header("Content-Disposition", ContentDisposition.inline().filename(name != null ? name : node.getTitle()).build().toString())
            .contentLength(attachment.getLength())
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .body(node.downloadAttachment(attachmentName == null ? "default" : attachmentName));        
    }

    /**
     * Return a preview of the requested node's attachment
     * 
     * @param id
     * @param branch
     * @param locale
     * @param attachmentName
     * @param name
     * @param metadata
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/preview/{id}")
    public @ResponseBody ResponseEntity<byte[]> previewById(@PathVariable final String nodeId,
            @RequestParam(required = false) final String branchId,
            @RequestParam(required = true) final String attachmentName,
            @RequestParam(required = false, defaultValue = "default") final String name,
            @RequestParam(required = true) final String mimetype, @RequestParam(required = true) final String size,
            @RequestAttribute(required = false) final Boolean metadata) throws Exception {
        
        log.debug("{} {} {}", branchId, attachmentName, nodeId);

        return ResponseEntity.ok()
            .header("Content-Disposition", ContentDisposition.inline().filename(name).build().toString())
            .contentType(MediaType.parseMediaType(mimetype))
            .body(driver.getDocumentPreviewBytesById(branchId, nodeId, attachmentName, mimetype, size, CloudcmsDriver.USE_CACHE));        
    }

    /**
     * Return the default attachment (if found) of the node identified by path
     * 
     * @param id
     * @param branch
     * @param locale
     * @param metadata
     * @return
     */
    @GetMapping(value = "/static/path/{path}")
    public @ResponseBody ResponseEntity<byte[]> staticByPath(@PathVariable final String path,
            @RequestParam(required = false) final String branch, 
            @RequestParam(required = false) final String locale,
            @RequestParam(required = false) final String attachmentName,
            @RequestParam(required = false) final String name,
            @RequestAttribute(required = false) final Boolean metadata) throws CloudCmsDriverBranchNotFoundException {

        log.debug("{} {} {}", branch, locale, path);

        final Node node = driver.getNodeByPath(driver.getBranch(branch).getId(), driver.getLocale(locale), path, CloudcmsDriver.USE_CACHE);
        final Attachment attachment = node.listAttachments().get(attachmentName == null ? "default" : attachmentName);

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=" + (name == null ? name : node.getTitle()))
            .contentLength(attachment.getLength())
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .body(node.downloadAttachment(attachmentName == null ? "default" : attachmentName));        
    }

    // @GetMapping(value = "/preview/node/{id}")
    // public @ResponseBody String previewyNodeId(@PathVariable String node,
    // @RequestAttribute Boolean metadata) {
    // return "ok";
    // }

    // @GetMapping(value = "/preview/path/{path}")
    // public @ResponseBody String previewByPath(@PathVariable String path,
    // @RequestAttribute Boolean metadata) {
    // return "ok";
    // }
}