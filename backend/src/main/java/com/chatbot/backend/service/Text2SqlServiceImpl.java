package com.chatbot.backend.service;

import com.chatbot.backend.config.Text2SqlPrompts;
import com.chatbot.backend.config.aws.*;
import com.chatbot.backend.dto.response.Text2SqlResponse;
import com.chatbot.backend.exception.LlmResponseParseException;
import com.chatbot.backend.util.LlmJson;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Text2SqlServiceImpl implements Text2SqlService {

    private final BedrockService bedrockService;
    private final ObjectMapper objectMapper;

    @Override
    public Text2SqlResponse generate(String question) {
        String rawIntent = ask(Text2SqlPrompts.INTENT_CLASSIFICATION_PROMPT,
                "질문: " + question);

        String rawEntities = ask(Text2SqlPrompts.ENTITY_EXTRACTION_PROMPT,
                "질문: " + question + "\n의도: " + rawIntent);

        String rawSql = ask(Text2SqlPrompts.SQL_GENERATION_PROMPT,
                "질문: " + question + "\n의도: " + rawIntent + "\nEntity: " + rawEntities);

        JsonNode intent = LlmJson.parse(rawIntent, objectMapper);
        JsonNode entities = LlmJson.parse(rawEntities, objectMapper);
        JsonNode sqlNode = LlmJson.parse(rawSql, objectMapper);

        return new Text2SqlResponse(intent, entities, extractSql(sqlNode), extractParams(sqlNode));
    }

    private String extractSql(JsonNode sqlNode) {
        JsonNode sql = sqlNode.get("sql");
        if (sql == null || !sql.isTextual()) {
            throw new LlmResponseParseException();
        }
        return sql.asText();
    }

    private List<String> extractParams(JsonNode sqlNode) {
        JsonNode params = sqlNode.get("params");
        if (params == null || params.isNull()) {
            return List.of();
        }
        return objectMapper.convertValue(params, new TypeReference<List<String>>() {
        });
    }

    private String ask(String systemPrompt, String userText) {
        ConversationRequest request = new ConversationRequest(
                List.of(new Message("user", List.of(new ContentBlock(userText)))),
                systemPrompt, null, null);

        ConversationResponse response = bedrockService.converse(request);
        if (response.getContent() == null || response.getContent().isEmpty()) {
            return "";
        }
        return response.getContent().get(0).getText();
    }
}
