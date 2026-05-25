package com.chatbot.backend.service;

import com.chatbot.backend.config.SystemPromptConfig;
import com.chatbot.backend.config.aws.*;
import com.chatbot.backend.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final BedrockService bedrockService;
    private final MessageHistoryService messageHistoryService;
    private final SessionManager sessionManager;

    public void stream(SseEmitter emitter, ChatRequest request) {
        String sessionId = request.getSessionId();
        try {
            messageHistoryService.add(sessionId, ROLE_USER, request.getMessage());
            sessionManager.touch(sessionId);

            ConversationRequest conversationRequest = new ConversationRequest(
                    toAwsMessages(messageHistoryService.get(sessionId)),
                    resolveSystemPrompt(request), null, null);

            StringBuilder assistantText = new StringBuilder();
            bedrockService.converseStream(conversationRequest,
                    event -> handleEvent(emitter, event, assistantText));

            persistAssistantMessage(sessionId, assistantText.toString());
            emitter.complete();
        } catch (Exception e) {
            log.error("Chat streaming failed for session {}", sessionId, e);
            sendError(emitter, e.getMessage());
            emitter.complete();
        }
    }

    private String resolveSystemPrompt(ChatRequest request) {
        if (StringUtils.hasText(request.getSystemPrompt())) {
            return request.getSystemPrompt();
        }

        return SystemPromptConfig.getSystemPrompt(request.getPersona());
    }

    private void persistAssistantMessage(String sessionId, String content) {
        try {
            messageHistoryService.add(sessionId, ROLE_ASSISTANT, content);
        } catch (Exception e) {
            log.error("Failed to persist assistant message for session {} (length={})",
                    sessionId, content.length(), e);
        }
    }

    private void handleEvent(SseEmitter emitter, StreamEvent event, StringBuilder assistantText) {
        try {
            if (event instanceof TextDeltaEvent textEvent) {
                assistantText.append(textEvent.getText());
                send(emitter, "text", TextResponse.from(textEvent));
                return;
            }
            if (event instanceof ToolUseStartEvent toolEvent) {
                send(emitter, "tool_start", ToolStartResponse.from(toolEvent));
                return;
            }
            if (event instanceof MessageStopEvent stopEvent) {
                send(emitter, "done", DoneResponse.from(stopEvent));
                return;
            }
            if (event instanceof ErrorEvent errorEvent) {
                send(emitter, "error", ErrorResponse.of(errorEvent.getMessage()));
            }
        } catch (IOException e) {
            log.error("Failed to send SSE event", e);
        }
    }

    private void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data, MediaType.APPLICATION_JSON));
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            send(emitter, "error", ErrorResponse.of(message));
        } catch (IOException e) {
            log.error("Failed to send SSE error event", e);
        }
    }

    private List<com.chatbot.backend.config.aws.Message> toAwsMessages(List<com.chatbot.backend.domain.Message> history) {
        if (history == null) {
            return Collections.emptyList();
        }

        return history.stream()
                .map(message -> new com.chatbot.backend.config.aws.Message(
                        message.getRole(),
                        List.of(new ContentBlock(message.getContent()))))
                .toList();
    }
}
