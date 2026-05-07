package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageStopEvent implements StreamEvent {
    private final String stopReason;
    private final TokenUsage usage;

    @Override
    public String getType() {
        return "messageStop";
    }
}
