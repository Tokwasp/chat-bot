package com.chatbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateSessionRequest {

    @NotBlank(message = "사용자 ID가 필요합니다.")
    private String userId;

    private String id;
    private String title;
    private Map<String, Object> metadata;
}
