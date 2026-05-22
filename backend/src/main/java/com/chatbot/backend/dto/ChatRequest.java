package com.chatbot.backend.dto;

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
public class ChatRequest {

    @NotBlank(message = "세션 ID가 필요합니다.")
    private String sessionId;

    @NotBlank(message = "메시지를 입력해주세요.")
    private String message;

    private String systemPrompt;
    private String persona;
}
