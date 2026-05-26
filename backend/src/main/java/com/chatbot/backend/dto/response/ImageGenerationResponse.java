package com.chatbot.backend.dto.response;

import lombok.Getter;

@Getter
public class ImageGenerationResponse {

    private final String image;

    private ImageGenerationResponse(String image) {
        this.image = image;
    }

    public static ImageGenerationResponse from(String base64Image) {
        return new ImageGenerationResponse(base64Image);
    }
}
