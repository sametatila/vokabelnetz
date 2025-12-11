package com.vokabelnetz.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a revoked token is reused.
 * This indicates potential token theft (SECURITY.md).
 */
public class TokenReusedException extends ApiException {

    public TokenReusedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "TOKEN_REUSED");
    }
}
