package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ConversationResponse {
    private final List<ContentBlock> content;
    private final String stopReason;
    private final TokenUsage usage;
}
