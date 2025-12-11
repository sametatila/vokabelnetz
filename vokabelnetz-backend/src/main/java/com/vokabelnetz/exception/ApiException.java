package com.vokabelnetz.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for API errors.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ApiException(String message, HttpStatus status) {
        this(message, status, status.name());
    }
}
