package com.chatbot.backend.config.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageGenerationResult {
    private final String imageBase64;
    private final Long seed;
    private final String mimeType;
}
