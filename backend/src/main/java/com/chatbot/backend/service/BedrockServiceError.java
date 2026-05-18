package com.chatbot.backend.service;

public class BedrockServiceError extends RuntimeException {

    private final boolean retryable;

    public BedrockServiceError(String message) {
        this(message, false);
    }

    public BedrockServiceError(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
