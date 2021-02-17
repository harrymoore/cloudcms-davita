package com.cloudcms.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends Exception {
    private static final long serialVersionUID = 1L;

    public ForbiddenException() {
        super("No Access Granted");
    }
    public ForbiddenException(final String message) {
        super(message);
    }
}