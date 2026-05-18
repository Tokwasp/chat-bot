package com.chatbot.backend.service;

import java.util.List;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;

public interface ToolOrchestrator {

    String executeTool(String toolName, String inputJson);

    List<Tool> getToolDefinitions();
}
