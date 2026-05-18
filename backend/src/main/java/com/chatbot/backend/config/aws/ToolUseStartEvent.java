package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ToolUseStartEvent implements StreamEvent {

    private final String toolUseId;
    private final String toolName;

    @Override
    public String getType() {
        return "tool_use_start";
    }
}
