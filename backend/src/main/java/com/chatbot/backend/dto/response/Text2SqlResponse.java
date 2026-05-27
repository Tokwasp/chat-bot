package com.chatbot.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Text2SqlResponse {
    private final String intent;
    private final String entities;
    private final String sql;
}
