package com.chatbot.backend.util;

import com.chatbot.backend.exception.LlmResponseParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LlmJson {

    private LlmJson() {
    }

    public static JsonNode parse(String raw, ObjectMapper mapper) {
        if (raw == null || raw.isBlank()) {
            log.error("LLM response is empty");
            throw new LlmResponseParseException();
        }

        String cleaned = stripFence(raw);
        try {
            return mapper.readTree(cleaned);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM JSON response: {}", raw, e);
            throw new LlmResponseParseException();
        }
    }

    // 모델이 가끔 ```json ... ``` 코드펜스로 감싸 응답하므로 벗겨낸다.
    private static String stripFence(String raw) {
        String cleaned = raw.strip();
        cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
        cleaned = cleaned.replaceFirst("\\s*```$", "");
        return cleaned.strip();
    }
}
