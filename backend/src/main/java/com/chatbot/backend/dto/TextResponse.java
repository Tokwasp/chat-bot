package com.chatbot.backend.dto;

import com.chatbot.backend.config.aws.TextDeltaEvent;
import lombok.Getter;

@Getter
public class TextResponse {

    private final String text;

    private TextResponse(String text) {
        this.text = text;
    }

    public static TextResponse from(TextDeltaEvent event) {
        return new TextResponse(event.getText());
    }
}
