package com.cloudcms.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason="Not authorized to view this document")
public class ForbiddenException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}