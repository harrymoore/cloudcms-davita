/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.server;

public class CmsDriverException extends Exception {
	private static final long serialVersionUID = 1L;

    public CmsDriverException(final String errorMessage) {
        super(errorMessage);
    }
}
