package com.example.graphqldemo.exception;


import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    private final List<String> errors;
    private final Map<String, String> invalidFields;

    public ValidationException(List<String> errors, Map<String, String> invalidFields) {
        super("Validation failed");
        this.errors = errors;
        this.invalidFields = invalidFields;
    }

    public ValidationException(String message, List<String> errors, Map<String, String> invalidFields) {
        super(message);
        this.errors = errors;
        this.invalidFields = invalidFields;
    }
}
