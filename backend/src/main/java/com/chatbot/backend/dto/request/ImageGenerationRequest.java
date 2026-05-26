package com.chatbot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageGenerationRequest {

    @NotBlank(message = "이미지 설명을 입력해주세요.")
    private String prompt;

    private String negativePrompt;
    private String aspectRatio;
    private Long seed;
}
