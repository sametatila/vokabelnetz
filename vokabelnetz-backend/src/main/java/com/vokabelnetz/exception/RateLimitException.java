package com.vokabelnetz.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class RateLimitException extends ApiException {

    public RateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS");
    }

    public RateLimitException() {
        super("Please wait a moment before trying again", HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS");
    }
}
