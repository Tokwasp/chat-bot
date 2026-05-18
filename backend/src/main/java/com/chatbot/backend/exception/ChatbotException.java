package com.chatbot.backend.exception;

import lombok.Getter;

@Getter
public class ChatbotException extends RuntimeException {

    private final int statusCode;
    private final String code;

    public ChatbotException(String message, int statusCode, String code) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }
}
