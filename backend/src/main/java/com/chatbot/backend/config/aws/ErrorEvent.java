package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorEvent implements StreamEvent {
    private final String message;

    @Override
    public String getType() {
        return "error";
    }
}
