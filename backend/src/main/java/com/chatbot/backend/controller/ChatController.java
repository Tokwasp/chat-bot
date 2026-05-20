package com.chatbot.backend.controller;

import com.chatbot.backend.config.SystemPromptConfig;
import com.chatbot.backend.config.aws.*;
import com.chatbot.backend.dto.ChatRequest;
import com.chatbot.backend.exception.ChatbotException;
import com.chatbot.backend.service.BedrockService;
import com.chatbot.backend.service.MessageHistoryService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private static final long SSE_TIMEOUT_MS = 300_000L;
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final int CORE_POOL_SIZE = 8;
    private static final int MAX_POOL_SIZE = 50;

    private final BedrockService bedrockService;
    private final MessageHistoryService messageHistoryService;
    private final ThreadPoolTaskExecutor streamExecutor = createStreamExecutor();

    @PostMapping
    public SseEmitter chat(@RequestBody ChatRequest request) {
        validate(request);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.warn("SSE connection error for session {}", request.getSessionId(), e));
        streamExecutor.execute(() -> stream(emitter, request));

        return emitter;
    }

    @PreDestroy
    void shutdownExecutor() {
        streamExecutor.shutdown();
    }

    private static ThreadPoolTaskExecutor createStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("chat-sse-");
        executor.initialize();

        return executor;
    }

    private void validate(ChatRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new ChatbotException("세션 ID가 필요합니다.", HttpStatus.BAD_REQUEST.value(), "MISSING_SESSION_ID");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ChatbotException("메시지를 입력해주세요.", HttpStatus.BAD_REQUEST.value(), "MISSING_MESSAGE");
        }
    }

    private String resolveSystemPrompt(ChatRequest request) {
        if (StringUtils.hasText(request.getSystemPrompt())) {
            return request.getSystemPrompt();
        }

        return SystemPromptConfig.getSystemPrompt(request.getPersona());
    }

    private void stream(SseEmitter emitter, ChatRequest request) {
        String sessionId = request.getSessionId();
        try {
            messageHistoryService.add(sessionId, ROLE_USER, request.getMessage());

            ConversationRequest conversationRequest = new ConversationRequest(
                    toAwsMessages(messageHistoryService.get(sessionId)),
                    resolveSystemPrompt(request), null, null);

            StringBuilder assistantText = new StringBuilder();
            bedrockService.converseStream(conversationRequest,
                    event -> handleEvent(emitter, event, assistantText));

            messageHistoryService.add(sessionId, ROLE_ASSISTANT, assistantText.toString());
            emitter.complete();
        } catch (Exception e) {
            log.error("Chat streaming failed for session {}", sessionId, e);
            sendError(emitter, e.getMessage());
            emitter.complete();
        }
    }

    private void handleEvent(SseEmitter emitter, StreamEvent event, StringBuilder assistantText) {
        try {
            if (event instanceof TextDeltaEvent textEvent) {
                assistantText.append(textEvent.getText());
                send(emitter, "text", Map.of("text", textEvent.getText()));
                return;
            }
            if (event instanceof ToolUseStartEvent toolEvent) {
                send(emitter, "tool_start", toolStartPayload(toolEvent));
                return;
            }
            if (event instanceof MessageStopEvent stopEvent) {
                send(emitter, "done", donePayload(stopEvent));
                return;
            }
            if (event instanceof ErrorEvent errorEvent) {
                send(emitter, "error", errorPayload(errorEvent.getMessage()));
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
            send(emitter, "error", errorPayload(message));
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

    private Map<String, Object> toolStartPayload(ToolUseStartEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("toolUseId", event.getToolUseId());
        data.put("toolName", event.getToolName());

        return data;
    }

    private Map<String, Object> donePayload(MessageStopEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("stopReason", event.getStopReason());
        data.put("usage", event.getUsage());

        return data;
    }

    private Map<String, Object> errorPayload(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message == null ? "" : message);

        return data;
    }
}
