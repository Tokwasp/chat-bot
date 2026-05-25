package com.chatbot.backend.dto;

import com.chatbot.backend.config.aws.ToolUseStartEvent;
import lombok.Getter;

@Getter
public class ToolStartResponse {

    private final String toolUseId;
    private final String toolName;

    private ToolStartResponse(String toolUseId, String toolName) {
        this.toolUseId = toolUseId;
        this.toolName = toolName;
    }

    public static ToolStartResponse from(ToolUseStartEvent event) {
        return new ToolStartResponse(event.getToolUseId(), event.getToolName());
    }
}
