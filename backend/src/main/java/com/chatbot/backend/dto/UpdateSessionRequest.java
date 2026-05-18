package com.chatbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class UpdateSessionRequest {

    private final String title;
    private final Map<String, Object> metadata;
}
