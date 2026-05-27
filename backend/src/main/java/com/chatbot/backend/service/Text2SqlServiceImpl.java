package com.chatbot.backend.service;

import com.chatbot.backend.config.Text2SqlPrompts;
import com.chatbot.backend.config.aws.ContentBlock;
import com.chatbot.backend.config.aws.ConversationRequest;
import com.chatbot.backend.config.aws.ConversationResponse;
import com.chatbot.backend.config.aws.Message;
import com.chatbot.backend.dto.response.Text2SqlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Text2SqlServiceImpl implements Text2SqlService {

    private final BedrockService bedrockService;

    @Override
    public Text2SqlResponse generate(String question) {
        String intent = ask(Text2SqlPrompts.INTENT_CLASSIFICATION_PROMPT,
                "질문: " + question);

        String entities = ask(Text2SqlPrompts.ENTITY_EXTRACTION_PROMPT,
                "질문: " + question + "\n의도: " + intent);

        String sql = ask(Text2SqlPrompts.SQL_GENERATION_PROMPT,
                "질문: " + question + "\n의도: " + intent + "\nEntity: " + entities);

        return new Text2SqlResponse(intent, entities, sql);
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
