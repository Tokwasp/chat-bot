package com.chatbot.backend.dto.response;

import com.chatbot.backend.config.aws.MessageStopEvent;
import com.chatbot.backend.config.aws.TokenUsage;
import lombok.Getter;

@Getter
public class DoneResponse {

    private final String stopReason;
    private final TokenUsage usage;

    private DoneResponse(String stopReason, TokenUsage usage) {
        this.stopReason = stopReason;
        this.usage = usage;
    }

    public static DoneResponse from(MessageStopEvent event) {
        return new DoneResponse(event.getStopReason(), event.getUsage());
    }
}
