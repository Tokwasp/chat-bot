package com.chatbot.backend.dto.response;

import lombok.Getter;

@Getter
public class ErrorResponse {

    private final String message;

    private ErrorResponse(String message) {
        this.message = message;
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message == null ? "" : message);
    }
}
