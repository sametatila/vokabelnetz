package com.vokabelnetz.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exception thrown when password doesn't meet policy requirements.
 */
@Getter
public class PasswordPolicyException extends ApiException {

    private final List<String> errors;

    public PasswordPolicyException(List<String> errors) {
        super("Password does not meet policy requirements", HttpStatus.BAD_REQUEST, "PASSWORD_POLICY_VIOLATION");
        this.errors = errors;
    }

    public PasswordPolicyException(String error) {
        this(List.of(error));
    }
}
