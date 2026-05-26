package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageGenerationRequest {
    private final String prompt;
    private final String negativePrompt;
    private final String aspectRatio;
    private final Long seed;
}
