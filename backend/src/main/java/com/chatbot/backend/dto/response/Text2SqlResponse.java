package com.chatbot.backend.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Text2SqlResponse {
    private final JsonNode intent;
    private final JsonNode entities;
    private final String sql;
    private final List<String> params;
}
