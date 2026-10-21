package com.archmaxim.exception;

import org.springframework.http.HttpStatus;

public class EmbeddingUnavailableException extends ApiException {

    public EmbeddingUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
        initCause(cause);
    }

    public EmbeddingUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
