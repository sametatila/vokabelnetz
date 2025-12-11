package com.vokabelnetz.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown for invalid or expired tokens.
 */
public class InvalidTokenException extends ApiException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
    }
}
