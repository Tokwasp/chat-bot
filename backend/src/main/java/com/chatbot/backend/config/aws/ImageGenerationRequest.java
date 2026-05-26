package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageGenerationRequest {
    private final String prompt;
    private final String negativePrompt;
    private final Integer width;
    private final Integer height;
    private final Integer cfgScale;
    private final Integer steps;
    private final Long seed;
}
