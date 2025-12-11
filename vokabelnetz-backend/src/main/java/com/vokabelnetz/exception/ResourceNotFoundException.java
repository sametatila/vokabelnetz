package com.vokabelnetz.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
}
