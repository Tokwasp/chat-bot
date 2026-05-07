package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenUsage {
    private final int inputTokens;
    private final int outputTokens;
}
