/**
 * Copyright (C) 2021 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import java.time.Duration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloudcms.server.CloudcmsDriver;
import com.cloudcms.server.CmsDriverBranchNotFoundException;

import org.gitana.platform.client.attachment.Attachment;
import org.gitana.platform.client.node.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
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
    // private final Logger log = LoggerFactory.getLogger(getClass());

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
            @RequestAttribute(required = false) final Boolean metadata) throws CmsDriverBranchNotFoundException {

        final Node node = driver.getNodeById(driver.getBranch(branchId).getId(), nodeId, CloudcmsDriver.USE_CACHE);
        final Attachment attachment = driver.getNodeAttachmentById(driver.getBranch(branchId).getId(), nodeId,
                attachmentId == null ? "default" : attachmentId, CloudcmsDriver.USE_CACHE);
        final byte[] bytes = driver.getDocumentAttachmentBytesById(driver.getBranch(branchId).getId(), nodeId,
                attachmentId == null ? "default" : attachmentId, CloudcmsDriver.USE_CACHE);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        ContentDisposition.builder(disposition.equalsIgnoreCase("inline") ? "inline" : "attachment")
                                .filename(name != null ? name : node.getTitle()).build().toString())
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(60)).cachePrivate())
                .contentLength(attachment.getLength())
                .contentType(MediaType.parseMediaType(attachment.getContentType())).body(bytes);
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
     * @throws CmsDriverBranchNotFoundException
     * @throws Exception
     */
    @GetMapping(value = "/preview/{nodeId}")
    public @ResponseBody ResponseEntity<byte[]> previewById(final HttpServletRequest request, final HttpServletResponse response,
        @PathVariable final String nodeId,
        @RequestParam(required = false) final String branchId,
        @RequestParam(required = true) final String attachmentId, @RequestParam(required = false) final String name,
        @RequestParam(required = true) final String mimetype, @RequestParam(required = true) final String size,
        @RequestParam(required = false, defaultValue = "inline") final String disposition,
        @RequestAttribute(required = false) final Boolean metadata) throws CmsDriverBranchNotFoundException {

        final Node node = driver.getNodeById(driver.getBranch(branchId).getId(), nodeId, CloudcmsDriver.USE_CACHE);

        try {
            byte[] responseBytes = driver.getDocumentPreviewBytesById(driver.getBranch(branchId).getId(), nodeId, attachmentId,
                mimetype, size, CloudcmsDriver.USE_CACHE);

            return ResponseEntity.ok()
                .header("Content-Disposition",
                    ContentDisposition.builder(disposition.equalsIgnoreCase("inline") ? "inline" : "attachment")
                .filename(name != null ? name : node.getTitle()).build().toString())
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(60)).cachePrivate())
                .contentLength(responseBytes.length).contentType(MediaType.parseMediaType(mimetype)).body(responseBytes);
        } catch (Exception e) {
            // could not generate a preview so just return original payload by redirecting to the static handler
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header("location", request.getRequestURL().toString().replace("/preview/", "/attachment/")).build();
        }
    }
}