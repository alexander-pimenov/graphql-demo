package com.example.graphqldemo.exception;

import lombok.Getter;

@Getter
public class CustomBusinessException extends RuntimeException {
    private final String errorCode;
    private final boolean retryable;

    public CustomBusinessException(String errorCode, boolean retryable) {
        super("CustomBusiness failed");
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public CustomBusinessException(String message, String errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
}
