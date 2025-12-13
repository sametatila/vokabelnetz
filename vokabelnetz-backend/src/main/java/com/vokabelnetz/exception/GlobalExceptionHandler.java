package com.vokabelnetz.exception;

import com.vokabelnetz.dto.response.ApiResponse;
import com.vokabelnetz.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = "Invalid request body";

        // Extract more specific error message
        Throwable cause = ex.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null) {
                if (causeMessage.contains("Unrecognized character escape")) {
                    message = "Invalid JSON: Special characters in strings must be properly escaped. " +
                              "Use double quotes for strings and escape special characters with backslash.";
                } else if (causeMessage.contains("Unexpected character")) {
                    message = "Invalid JSON format: Check for missing quotes, commas, or brackets.";
                } else if (causeMessage.contains("Cannot deserialize")) {
                    message = "Invalid field value: " + extractFieldFromMessage(causeMessage);
                }
            }
        }

        log.warn("JSON parse error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_JSON", message);
        error.setDetails(Map.of("hint", "Ensure your JSON is properly formatted. Example: {\"email\":\"user@example.com\",\"password\":\"YourPass123\"}"));
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("MISSING_PARAMETER", "Required parameter '" + ex.getParameterName() + "' is missing"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter {}: {}", ex.getName(), ex.getValue());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_PARAMETER", "Invalid value for parameter '" + ex.getName() + "'"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    /**
     * Extract field name from deserialization error message.
     */
    private String extractFieldFromMessage(String message) {
        // Try to extract field name from message like "Cannot deserialize value of type ... from String \"...\" for field `email`"
        if (message.contains("field")) {
            int fieldIndex = message.lastIndexOf("field");
            if (fieldIndex > 0 && fieldIndex + 10 < message.length()) {
                return message.substring(fieldIndex);
            }
        }
        return "Check your request body format";
    }
}
