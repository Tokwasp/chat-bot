package com.chatbot.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private final ErrorBody error;

    @Getter
    @AllArgsConstructor
    public static class ErrorBody {
        private final String message;
        private final String code;
    }
}
