package com.chatbot.backend.controller;

import com.chatbot.backend.config.aws.ImageGenerationResult;
import com.chatbot.backend.dto.request.ImageGenerationRequest;
import com.chatbot.backend.dto.response.ApiResponse;
import com.chatbot.backend.service.ImageGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageGenerationService imageGenerationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ImageGenerationResult>> generate(
            @Valid @RequestBody ImageGenerationRequest request) {
        ImageGenerationResult result = imageGenerationService.generate(toServiceRequest(request));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private com.chatbot.backend.config.aws.ImageGenerationRequest toServiceRequest(ImageGenerationRequest request) {
        return new com.chatbot.backend.config.aws.ImageGenerationRequest(
                request.getPrompt(),
                request.getNegativePrompt(),
                request.getAspectRatio(),
                request.getSeed());
    }
}
