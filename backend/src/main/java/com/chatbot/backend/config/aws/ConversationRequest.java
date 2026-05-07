package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ConversationRequest {
    private final List<Message> messages;
    private final String systemPrompt;
    private final Integer maxTokens;
    private final Double temperature;
}
