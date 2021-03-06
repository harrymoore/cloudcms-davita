/**
 * Copyright (C) 2021 Gitana Software, Inc.
 */
package com.cloudcms.controllers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloudcms.cache.CacheClearService;
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
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DocumentViewerController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${keycloak.enabled}")
    private boolean keycloakEnabled;

    @Value("${app.ui-tags:false}")
    private boolean useTags;

    @Value("${keycloak.resource}")
    private String keycloakResource;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthUrl;

    @Autowired
    private CloudcmsDriver driver;

    @Autowired
    private CacheClearService cacheClearService;

    @GetMapping(value = { "/" })
    public String redirectToRoot() {
        return "redirect:/documents";
    }

    private final String logout_url_template = "%s/realms/%s/protocol/openid-connect/auth?response_type=code&client_id=%s&redirect_uri=__base_url__&login=true&scope=openid";
    private String logout_url;

    private static final Pagination paginationAllSortByTarget = new Pagination();
    private static final Pagination paginationAllSortByTitle = new Pagination();
    private static final ObjectNode tagQuery = JsonUtil.createObject();

    static {
        // initialize reusable paging object
        paginationAllSortByTitle.setLimit(-1);
        paginationAllSortByTitle.getSorting().addSort("title", 1);

        // initialize reusable paging object
        paginationAllSortByTarget.setLimit(-1);
        paginationAllSortByTitle.getSorting().addSort("target", 1);

        // initialize reusable tag query (actually an a:has_tag association query)
        tagQuery.put("_type", "a:has_tag");
        tagQuery.put("source_type", CloudcmsDriver.NODE_TYPE);
    }

    @PostConstruct
    private synchronized void init() {
        logout_url = String.format(logout_url_template, keycloakAuthUrl, keycloakRealm, keycloakResource).replaceAll("\\/\\/", "\\/");
    }

    @GetMapping(value = { "/logout" })
    public String logout(HttpServletRequest request) {
        try {
            request.logout();
        } catch (ServletException e) {
            log.warn("Error while logging out of session {}", e.getMessage());
        }
        
        return "redirect:" + logout_url.replace("__base_url__", request.getRequestURL().substring(0, request.getHeader("host").length() + request.getRequestURL().lastIndexOf(request.getHeader("host"))));
    }

    @SuppressWarnings("unchecked")
    @GetMapping(value = { "/documents", "/documents/{nodeId}" })
    public String getDocument(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable(required = false) String nodeId, @RequestParam(required = false) final String branchId,
            @RequestParam(required = false, defaultValue = "") final String searchText,
            @RequestParam(required = false) final String rangeFilter,
            @RequestParam(required = false, defaultValue = "") final String tagFilter,
            @RequestParam(required = false, defaultValue = "true") final String useCache, 
            @RequestParam(required = false, defaultValue = "false") final String clearCache, 
            final Model map)
            throws CmsDriverBranchNotFoundException, ForbiddenException {

        log.info("getDocument {}", request.getRequestURI());

        if (Boolean.parseBoolean(clearCache)) {
            cacheClearService.clearCache();
        }

        response.setHeader("Cache-Control", CacheControl.maxAge(Duration.ofMinutes(60)).cachePrivate().toString());

        List<String> userRoles = Collections.emptyList();

        map.addAttribute("userEmail", "");
        map.addAttribute("userId", "");
        map.addAttribute("userName", "");
        map.addAttribute("userRoles", userRoles);

        if (keycloakEnabled) {
            if (request.getSession(false) == null) {
                log.info("No session");
                throw new ForbiddenException();
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

                if (userRoles.isEmpty()) {
                    log.info("User has no role");
                    throw new ForbiddenException();
                }
            }            
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
            if (keycloakEnabled) {
                if (node.get("entitlements") != null) {
                    for (String entitlement : (List<String>) node.get("entitlements")) {
                        if (userRoles.contains(entitlement)) {
                            entitled = true;
                            continue;
                        }
                    }
                }
            } else {
                entitled = true;
            }

            if (!entitled) {
                throw new ForbiddenException();
            }

            map.addAttribute("document", node);
            map.addAttribute("documentId", node.getId());

            // for each "document" relator item, gather info about the related node and it's
            // "default" attachment
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
            // find a:has_tag association nodes. This will naturally filter out any unused tags
            Set<String> tagNodeIds = new HashSet<>();
            driver.queryAssociations(driver.getBranch(branchId).getId(), tagQuery, paginationAllSortByTitle, cache).forEach(node -> {
                tagNodeIds.add((String)node.get("target"));
            });

            ObjectNode tagNodeQuery = JsonUtil.createObject();
            tagNodeQuery.put("_type", "n:tag");    
            tagNodeQuery.set("_doc", JsonUtil.createObject().set("$in", JsonUtil.createArray(tagNodeIds)));
            map.addAttribute("tags", driver.queryNodes(driver.getBranch(branchId).getId(), tagNodeQuery, paginationAllSortByTitle, cache));
        }

        return "index";
    }
}