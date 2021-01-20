/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.server;

public class CloudCmsDriverException extends Exception {
	private static final long serialVersionUID = 1L;

    public CloudCmsDriverException(final String errorMessage) {
        super(errorMessage);
    }
}
