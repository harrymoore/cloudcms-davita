/**
 * Copyright (C) 2021 Gitana Software, Inc.
 */
package com.cloudcms.server;

public class CmsDriverBranchNotFoundException extends CmsDriverException {
    private static final long serialVersionUID = 1L;

    public CmsDriverBranchNotFoundException(final String branchId) {
        super(String.format("Branch with id % not found", branchId));
    }
}