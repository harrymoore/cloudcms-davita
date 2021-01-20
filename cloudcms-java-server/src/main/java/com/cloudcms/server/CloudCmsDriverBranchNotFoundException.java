/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.server;

public class CloudCmsDriverBranchNotFoundException extends CloudCmsDriverException {
    private static final long serialVersionUID = 1L;

    public CloudCmsDriverBranchNotFoundException(final String branchId) {
        super(String.format("Branch with id % not found", branchId));
    }
}