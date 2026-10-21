package com.archmaxim.exception;

import org.springframework.http.HttpStatus;

import java.time.Duration;

public class TooManyAttemptsException extends ApiException {

    private final Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts. Try again in "
                + minutesUntil(retryAfter) + " minute(s).");
        this.retryAfter = retryAfter;
    }

    private static long minutesUntil(Duration duration) {
        return Math.max(1, (duration.toSeconds() + 59) / 60);
    }

    public long retryAfterSeconds() {
        return Math.max(1, retryAfter.toSeconds());
    }
}
