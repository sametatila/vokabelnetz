package com.vokabelnetz.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown for authentication failures.
 * Uses generic message to prevent account enumeration.
 */
public class AuthenticationException extends ApiException {

    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public AuthenticationException() {
        super("Invalid email or password", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
