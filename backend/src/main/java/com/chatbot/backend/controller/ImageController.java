package com.chatbot.backend.controller;

import com.chatbot.backend.dto.request.ImageGenerationRequest;
import com.chatbot.backend.dto.response.ApiResponse;
import com.chatbot.backend.dto.response.ImageGenerationResponse;
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
    public ResponseEntity<ApiResponse<ImageGenerationResponse>> generate(
            @Valid @RequestBody ImageGenerationRequest request) {
        String base64Image = imageGenerationService.generate(request);
        return ResponseEntity.ok(ApiResponse.ok(ImageGenerationResponse.from(base64Image)));
    }
}
