package com.archmaxim.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for errors that map directly to an HTTP status. The message is shown to
 * the user, so keep it short and free of internal details.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
