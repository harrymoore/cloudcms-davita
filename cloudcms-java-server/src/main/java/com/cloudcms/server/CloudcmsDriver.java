/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.gitana.platform.client.Driver;
import org.gitana.platform.client.Gitana;
import org.gitana.platform.client.application.Application;
import org.gitana.platform.client.attachment.Attachment;
import org.gitana.platform.client.branch.Branch;
import org.gitana.platform.client.node.Node;
import org.gitana.platform.client.platform.Platform;
import org.gitana.platform.client.project.Project;
import org.gitana.platform.client.repository.Repository;
import org.gitana.platform.client.support.DriverContext;
import org.gitana.platform.client.support.RemoteImpl;
import org.gitana.platform.services.branch.BranchType;
import org.gitana.platform.support.Pagination;
import org.gitana.platform.support.QName;
import org.gitana.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

// Spring will initialize as a singleton
@Service
@PropertySource(value = { "classpath:gitana-default.properties",
        "file:./gitana.properties" }, ignoreResourceNotFound = true)
public class CloudcmsDriver {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final Boolean USE_CACHE = Boolean.TRUE;
    public static final Boolean IGNORE_CACHE = Boolean.FALSE;
    public static final String NODE_TYPE = "davita:document";
    public static final QName NODE_TYPE_QNAME = QName.create(NODE_TYPE);

    @Value("${gitana.clientKey}")
    private String clientKey;

    @Value("${gitana.clientSecret}")
    private String clientSecret;

    @Value("${gitana.username}")
    private String username;

    @Value("${gitana.password}")
    private String password;

    @Value("${gitana.baseURL}")
    private String baseURL;

    @Value("${gitana.application}")
    private String applicationId;

    @Value("${gitana.locale}")
    private String defaultLocale;
    private Locale locale;

    @Value("${gitana.branch}")
    private String defaultBranch = "master";
    private Branch activeBranch;

    private Driver driver;
    private Platform platform;
    private Application application;
    private Project project;
    private Repository contentRepository;
    private Map<String, Branch> branchList = new HashMap<>();

    /**
     * Initialize the connection to Cloud CMS
     * 
     * @throws CmsDriverException
     */
    @PostConstruct
    private synchronized void init() throws CmsDriverBranchNotFoundException {
        log.info(String.format("Initializing connection to cloud cms api server %s", baseURL));
        platform = Gitana.connect(clientKey, clientSecret, username, password, baseURL);
        application = platform.readApplication(applicationId);
        log.info(String.format("Using application object with id \"%s\"", application.getId()));
        project = platform.readProject(application.get("projectId").toString());
        log.info(String.format("Connected to project \"%s\" with id \"%s\"", project.getTitle(), project.getId()));
        contentRepository = (Repository) project.getStack().readDataStore("content");

        driver = DriverContext.getDriver();
        ((RemoteImpl) driver.getRemote()).setPaths(false);
        ((RemoteImpl) driver.getRemote()).setMetadata(true);
        ((RemoteImpl) driver.getRemote()).setFull(true);
        locale = new Locale(defaultLocale);
        DriverContext.getDriver().setLocale(locale);

        refreshBranches();
        activeBranch = getBranch(defaultBranch);
    }

    /**
     * retrieve the list of active workspace branches from Cloud CMS. Sets
     * this.branchList keyed by branch id and by branch title
     */
    private synchronized void refreshBranches() {
        branchList.keySet().removeAll(branchList.keySet());
        contentRepository.listBranches().asList().forEach(branch -> {
            if (branch.isMaster()) {
                branchList.put("master", branch);
            } else if (branch.getType().equals(BranchType.CUSTOM) && !branch.isReadOnly() && !branch.isSnapshot()
                    && !branch.isFrozen() && !branch.getTitle().equals("")) {
                // this looks like an active workspace so index it by name similar to master
                branchList.put(branch.getTitle(), branch);
            }

            branchList.put(branch.getId(), branch);
        });
    }

    /**
     * Retrieve the active branch or master
     * 
     * @return the requested Branch (master) on the current Repository
     */
    public Branch getBranch() {
        DriverContext.setDriver(driver);

        return activeBranch != null ? activeBranch : branchList.get("master");
    }

    /**
     * Retrieve the requested branch from list of existing workspace branches
     * 
     * @return the requested {@code Branch} on the current Repository
     */
    public Branch getBranch(final String branchId) throws CmsDriverBranchNotFoundException {
        DriverContext.setDriver(driver);

        if (branchId == null || branchId.equals("")) {
            return activeBranch != null ? activeBranch : branchList.get("master");
        }

        if (!branchList.containsKey(branchId)) {
            refreshBranches();

            if (!branchList.containsKey(branchId)) {
                throw new CmsDriverBranchNotFoundException(branchId);
            }
        }

        // hand back the branch
        return branchList.get(branchId);
    }

    /**
     * retrieve a node by its id (_doc)
     * 
     * @param branchId
     * @param nodeId
     * @param cacheResults
     * @return
     * @throws CmsDriverBranchNotFoundException
     */
    @Cacheable(value = "node-cache", condition = "#cacheResults.equals(true)", key = "#root.methodName.concat(#branchId).concat(#nodeId)")
    public Node getNodeById(final String branchId, final String nodeId, final Boolean cacheResults)
            throws CmsDriverBranchNotFoundException {
        log.debug(String.format("get node with id %s from branch %s", nodeId, branchId));

        return (Node) getBranch(branchId).readNode(nodeId);
    }

    /**
     * query for nodes by type qname (ex.: "n:node")
     * 
     * @param branchId
     * @param idList
     * @param roleFilter
     * @param rangeFilter
     * @param tagFilter
     * @param type
     * @param cacheResults
     * @return
     * @throws CmsDriverBranchNotFoundException
     */
    @Cacheable(value = "query-cache", condition = "#cacheResults.equals(true)", key = "#root.methodName.concat(#branchId).concat(#idList.toString()).concat(#roleFilter==null?'':#roleFilter.toString()).concat(#rangeFilter==null?'':#rangeFilter).concat(#tagFilter==null?'':#tagFilter).concat(#type)")
    public List<Node> queryNodesByType(final String branchId, final List<String> idList, final List<String> roleFilter,
            final String rangeFilter, final String tagFilter, final String type, final Boolean cacheResults)
            throws CmsDriverBranchNotFoundException {
        log.debug(String.format("query nodes by type %s", type));

        Pagination pagination = new Pagination();
        pagination.setSkip(0);
        pagination.setLimit(-1);
        pagination.getSorting().addSort("_system.modified_on.ms", -1);

        ObjectNode query = JsonUtil.createObject();
        query.put("_type", type);

        if (!idList.isEmpty()) {
            query.set("_doc", JsonUtil.createObject().set("$in", JsonUtil.createArray(idList)));
        }

        if (!roleFilter.isEmpty()) {
            query.set("entitlements", JsonUtil.createObject().set("$in", JsonUtil.createArray(roleFilter)));
        }

        if (rangeFilter != null) {
            long ms = (System.currentTimeMillis() * 1000) - (Integer.parseInt(rangeFilter) * 86400000l);
            query.set("_system.modified_on.ms", JsonUtil.createObject().put("$gt", ms));
        }

        query.set("_fields", JsonUtil.createObject().put("title", 1).put("_type", 1).put("_qname", 1)
                .put("_system.modified_on.iso_8601", 1));

        if (!tagFilter.isEmpty()) {
            query.put("tags", tagFilter);
            ((ObjectNode) query.get("_fields")).put("tags", 1);
        }

        List<Node> list = new ArrayList<>(1000);
        getBranch(branchId).queryNodes(query, pagination).forEach((k, n) -> list.add((Node) n));

        return list;
    }

    /**
     * search for nodes on a branch
     * 
     * @param branchId
     * @param text
     * @param cacheResults
     * @return
     * @throws CmsDriverBranchNotFoundException
     */
    @Cacheable(value = "query-cache", condition = "#cacheResults.equals(true)", key = "#root.methodName.concat(#branchId).concat(#text == null ? '' : #text)")
    public List<Node> searchNodes(final String branchId, final String text, final Boolean cacheResults)
            throws CmsDriverBranchNotFoundException {
        log.debug("search nodes by for string {}", text);

        ObjectNode search = JsonUtil.createObject();
        search.set("search", JsonUtil.createObject().set("query",
                JsonUtil.createObject().set("query_string", JsonUtil.createObject().put("query", text))));
        search.set("_fields", JsonUtil.createObject().put("title", 1).put("_type", 1).put("_qname", 1));

        List<Node> list = new ArrayList<>();
        getBranch(branchId).searchNodes(search).forEach((k, n) -> list.add((Node) n));

        return list;
    }

    private static final String[] types = { NODE_TYPE };
    private static final ObjectNode traverse = JsonUtil.createObject();
    static {
        traverse.set("associations", JsonUtil.createObject().put("a:linked", "INCOMING"));
        traverse.put("depth", 1);
        traverse.set("types", JsonUtil.createArray(types));
    }

    /**
     * find nodes on a branch. uses the node "find" API call rather than the branch
     * "find" so the tranverse option can be used
     * 
     * @param branchId
     * @param text
     * @param cacheResults
     * @return
     * @throws CmsDriverBranchNotFoundException
     */
    @Cacheable(value = "query-cache", condition = "#cacheResults.equals(true)", key = "#root.methodName.concat(#branchId).concat(#text == null ? '' : #text)")
    public List<Node> findNodes(final String branchId, final String text, final Boolean cacheResults)
            throws CmsDriverBranchNotFoundException {
        log.debug("find nodes with string {}", text);

        List<Node> list = new ArrayList<>();
        getBranch(branchId).rootNode().findNodes(null, text, traverse).forEach((k, n) -> list.add((Node) n));

        return list;
    }

    /**
     * query for nodes using an arbitrary query passed in.
     * 
     * @param branch
     * @param query
     * @return List<Node>
     * @throws CmsDriverBranchNotFoundException
     */
    @Cacheable(value = "query-cache", condition = "#cacheResults.equals(true)", key = "#root.methodName.concat(#branchId).concat(#query.toString())")
    public List<Node> queryNodes(final String branchId, final ObjectNode query, final Pagination pagination,
            final Boolean cacheResults) throws CmsDriverBranchNotFoundException {
        log.debug(String.format("query nodes %s", query));

        final Branch branch = getBranch(branchId);

        List<Node> list = new ArrayList<>(100);
        branch.queryNodes(query, pagination != null ? pagination : new Pagination(0, 100))
                .forEach((k, n) -> list.add((Node) n));

        return list;
    }

    /**
     * retrieve a node's attachment object
     * 
     * @param branchId
     * @param nodeId
     * @param attachmentId
     * @param cacheResults
     * @return
     * @throws CmsDriverBranchNotFoundException
     */
    @Cacheable(value = "attachment-cache", condition = "#cacheResults.equals(true)", key = "#root.methodName.concat(#branchId).concat(#nodeId).concat(#attachmentId)")
    public Attachment getNodeAttachmentById(final String branchId, final String nodeId, final String attachmentId,
            final Boolean cacheResults) throws CmsDriverBranchNotFoundException {

        return getBranch(branchId).readNode(nodeId).listAttachments().get(attachmentId);
    }

    /**
     * retrieve a node's attachment bytes
     * 
     * @param branchId
     * @param nodeId
     * @param attachmentId
     * @param cacheResults
     * @return
     * @throws CmsDriverBranchNotFoundException
     */
    @Cacheable(value = "attachment-cache", condition = "#cacheResults.equals(true)", key = "#root.methodName.concat(#branchId).concat(#nodeId).concat(#attachmentId)")
    public byte[] getDocumentAttachmentBytesById(final String branchId, final String nodeId, final String attachmentId,
            final Boolean cacheResults) throws CmsDriverBranchNotFoundException {

        return getBranch(branchId).readNode(nodeId).downloadAttachment(attachmentId);
    }

    /**
     * retrieve a node's attachment preview bytes
     * 
     * @param branchId
     * @param nodeId
     * @param attachmentId
     * @param mimetype
     * @param size
     * @param cacheResults
     * @return
     * @throws Exception
     */
    @Cacheable(value = "attachment-cache", condition = "#cacheResults.equals(true)", key = "#root.methodName.concat(#branchId).concat(#nodeId).concat(#attachmentId).concat(#mimetype).concat(#mimetype).concat(#size)")
    public byte[] getDocumentPreviewBytesById(final String branchId, final String nodeId, final String attachmentId,
            final String mimetype, final String size, final Boolean cacheResults) throws Exception {

        // generate the preview
        final String previewUrl = String.format(
                "/repositories/%s/branches/%s/nodes/%s/preview/_davita_%s_%s?attachment=%s&mimetype=%s&size=%s",
                contentRepository.getId(), branchId, nodeId, attachmentId, size, attachmentId, mimetype, size);

        return getDriver().getRemote().downloadBytes(previewUrl);
    }

    /**
     * sets the locale of the driver for all future API calls.
     * 
     * @param locale
     */
    public void setLocale(String locale) {
        if (locale == null || locale.equals("")) {
            locale = "default";
        }

        driver.setLocale(new Locale(locale));
        DriverContext.setDriver(driver);
    }

    public String getLocale() {
        return locale.toString();
    }

    /**
     * Return either the current locale or the specified locale
     * 
     * @param locale
     * @return
     */
    public String getLocale(String requestedLocale) {
        return requestedLocale != null ? requestedLocale : locale.toString();
    }

    public Driver getDriver() {
        return driver;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Application getApplication() {
        return application;
    }

    public String getApplicationId() {
        return application.getId();
    }

    public Project getProject() {
        return project;
    }

    public Repository getContentRepository() {
        return contentRepository;
    }

    public Branch getMaster() {
        DriverContext.setDriver(driver);

        return branchList.get("master");
    }

    public Map<String, Branch> getBranchList() {
        DriverContext.setDriver(driver);

        return branchList;
    }
}