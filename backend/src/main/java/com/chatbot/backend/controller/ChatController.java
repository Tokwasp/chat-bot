package com.chatbot.backend.controller;

import com.chatbot.backend.dto.request.ChatRequest;
import com.chatbot.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final ChatService chatService;
    private final ThreadPoolTaskExecutor streamExecutor;

    @PostMapping
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.warn("SSE connection error for session {}", request.getSessionId(), e));
        streamExecutor.execute(() -> chatService.stream(emitter, request));

        return emitter;
    }
}
