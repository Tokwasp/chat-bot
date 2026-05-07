package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TextDeltaEvent implements StreamEvent {
    private final String text;

    @Override
    public String getType() {
        return "textDelta";
    }
}
