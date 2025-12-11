package com.vokabelnetz.exception;

import com.vokabelnetz.dto.response.ApiResponse;
import com.vokabelnetz.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.warn("API Exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
            .status(ex.getStatus())
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(PasswordPolicyException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordPolicyException(PasswordPolicyException ex) {
        log.warn("Password policy violation: {}", ex.getErrors());
        ErrorResponse error = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        error.setDetails(Map.of("errors", ex.getErrors()));
        return ResponseEntity
            .status(ex.getStatus())
            .body(ApiResponse.error(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", "Validation failed");
        error.setDetails(Map.of("fields", errors));
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("FORBIDDEN", "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
