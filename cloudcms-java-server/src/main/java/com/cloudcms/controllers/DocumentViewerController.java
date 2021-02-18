/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloudcms.server.CloudcmsDriver;
import com.cloudcms.server.CmsDriverBranchNotFoundException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.gitana.platform.client.attachment.Attachment;
import org.gitana.platform.client.node.Association;
import org.gitana.platform.client.node.Node;
import org.gitana.platform.client.node.type.LinkedAssociation;
import org.gitana.platform.services.association.Direction;
import org.gitana.platform.support.Pagination;
import org.gitana.util.JsonUtil;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken.Access;
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

    @Value("${app.ui-tags:false}")
    private boolean useTags;

    @Value("${keycloak.resource}")
    private String keycloakResource;

    @Autowired
    private CloudcmsDriver driver;

    @GetMapping(value = { "/" })
    public String redirectToRoot() {
        return "redirect:/documents";
    }

    // @GetMapping(value = { "/logout" })
    // public String logout(HttpServletRequest request) throws ServletException {
    // request.logout();
    // return "redirect:/documents";
    // }

    @GetMapping(value = { "/documents", "/documents/{nodeId}" })
    public String getDocument(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable(required = false) String nodeId, @RequestParam(required = false) final String branchId,
            @RequestParam(required = false, defaultValue = "") final String searchText,
            @RequestParam(required = false) final String rangeFilter,
            @RequestParam(required = false, defaultValue = "") final String tagFilter,
            @RequestParam(required = false, defaultValue = "true") final String useCache, final Model map)
            throws CmsDriverBranchNotFoundException, ForbiddenException {

        log.info("getDocument {}", request.getRequestURI());

        List<String> userRoles = Collections.emptyList();

        if (request.getSession(false) == null) {
            log.info("No session");
            map.addAttribute("userEmail", "");
            map.addAttribute("userId", "");
            map.addAttribute("userName", "");
            map.addAttribute("userRoles", "");
        } else {
            KeycloakPrincipal<?> principal = (KeycloakPrincipal<?>) request.getUserPrincipal();
            map.addAttribute("userEmail", principal.getKeycloakSecurityContext().getToken().getEmail());
            map.addAttribute("userId", principal.getKeycloakSecurityContext().getToken().getId());

            Access resourceAccess = principal.getKeycloakSecurityContext().getToken()
                    .getResourceAccess(keycloakResource);

            if (resourceAccess.getRoles() != null && !resourceAccess.getRoles().isEmpty()) {
                userRoles = new ArrayList<String>(resourceAccess.getRoles());
            }

            log.info("Session user {} with roles {}", principal.getName(), userRoles);
            map.addAttribute("userName", principal.getName());
            map.addAttribute("userRoles", String.join(",", userRoles));
        }

        map.addAttribute("searchText", searchText == null ? "" : searchText);
        map.addAttribute("rangeFilter", rangeFilter == null ? "" : rangeFilter);
        map.addAttribute("tagFilter", tagFilter == null ? "" : tagFilter);

        final Boolean cache = Boolean.parseBoolean(useCache);

        List<Node> indexNodes = new ArrayList<Node>();

        /*
         * if there is a search string provided then first search for documents with the
         * search value and then find any davita:document instances that reference them
         * and further check those for other filters
         */
        if (searchText != null && !searchText.isBlank()) {
            final List<String> indexNodesList = new ArrayList<String>();
            final List<Node> searchResultNodes = driver.searchNodes(driver.getBranch(branchId).getId(), searchText, cache);
            for (Node node : searchResultNodes) {
                if (node.getTypeQName().equals(CloudcmsDriver.NODE_TYPE_QNAME)) {
                    // this is a document so include it in the index
                    indexNodesList.add(node.getId());
                } else {
                    // this is a different document type. see if it has an association to a
                    // davita:document
                    for (Association association : node.associations(LinkedAssociation.QNAME, Direction.ANY).asList()) {
                        // log.debug("{}", association.toString());
                        if (association.getSourceNodeTypeQName().equals(CloudcmsDriver.NODE_TYPE_QNAME)) {
                            // this document is related to a davita document
                            indexNodesList.add(association.getSourceNodeId());
                        }
                    }
                }
            }

            // add the list of documents to the model so that an index can be built
            if (!indexNodesList.isEmpty()) {
                indexNodes = driver.queryNodesByType(driver.getBranch(branchId).getId(), indexNodesList,
                        userRoles, rangeFilter, tagFilter, CloudcmsDriver.NODE_TYPE, cache);

            }

            map.addAttribute("indexDocuments", indexNodes);
        } else {
            // add the list of documents to the model so that an index can be built
            indexNodes = driver.queryNodesByType(driver.getBranch(branchId).getId(),
                    Collections.<String>emptyList(), userRoles, rangeFilter, tagFilter, CloudcmsDriver.NODE_TYPE,
                    cache);
            map.addAttribute("indexDocuments", indexNodes);
        }

        Boolean hasVideo = false;
        Boolean hasAudio = false;
        Boolean hasPdf = false;
        Boolean hasImage = false;
        Boolean entitled = false;

        // add the requested document, if it exists, to the model
        if (null != nodeId && !nodeId.isEmpty()) {
            Node node = driver.getNodeById(driver.getBranch(branchId).getId(), nodeId, cache);

            // check that role assignments allow access to this document
            if (node.get("entitlements") != null) {
                for (Map<String, String> entitlement : (List<Map<String, String>>) node.get("entitlements")) {
                    // log.debug("role {}", entitlement.get("title"));
                    if (userRoles.contains(entitlement.get("title"))) {
                        entitled = true;
                        continue;
                    }
                }
            }

            if (!entitled) {
                throw new ForbiddenException();
            }

            map.addAttribute("document", node);
            map.addAttribute("documentId", node.getId());

            // for each "document" relator item, gather info about the related node and it's
            // "default" attachment
            @SuppressWarnings("unchecked")
            List<Map<String, String>> relatedDocuments = (List<Map<String, String>>) node.get("document");
            for (Map<String, String> doc : relatedDocuments) {
                Attachment attachment = driver.getNodeAttachmentById(driver.getBranch(branchId).getId(),
                        (String) doc.get("id"), "default", cache);

                boolean isOther = true;

                doc.put("id", doc.get("id"));
                doc.put("attachmentId", attachment.getId());
                doc.put("mimetype", attachment.getContentType());
                doc.put("isVideo",
                        String.valueOf(attachment.getContentType().startsWith("video/")
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
        } else {
            map.addAttribute("hasVideo", false);
            map.addAttribute("hasAudio", false);
            map.addAttribute("hasPdf", false);
            map.addAttribute("hasImage", false);
        }

        if (useTags) {
            ObjectNode query = JsonUtil.createObject();
            query.put("_type", "n:tag");
            query.set("_fields",
                    JsonUtil.createObject().put("title", 1).put("tag", 1).put("_type", 1).put("_qname", 1));

            Pagination pagination = new Pagination();
            pagination.setLimit(1000);
            pagination.getSorting().addSort("title", 1);

            map.addAttribute("tags", driver.queryNodes(driver.getBranch(branchId).getId(), query, pagination, cache));
        }

        return template;
    }
}