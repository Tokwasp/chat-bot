package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ToolResultEvent implements StreamEvent {

    private final String toolUseId;
    private final String toolResult;

    @Override
    public String getType() {
        return "tool_result";
    }
}
