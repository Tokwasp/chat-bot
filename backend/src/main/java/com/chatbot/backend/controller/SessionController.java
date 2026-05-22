package com.chatbot.backend.controller;

import com.chatbot.backend.dto.ApiResponse;
import com.chatbot.backend.dto.CreateSessionRequest;
import com.chatbot.backend.dto.MessageDto;
import com.chatbot.backend.dto.SessionDto;
import com.chatbot.backend.dto.UpdateSessionRequest;
import com.chatbot.backend.exception.ChatbotException;
import com.chatbot.backend.service.MessageHistoryService;
import com.chatbot.backend.service.SessionManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class SessionController {

    private static final String SESSION_NOT_FOUND_MESSAGE = "세션을 찾을 수 없습니다.";
    private static final String SESSION_NOT_FOUND_CODE = "SESSION_NOT_FOUND";

    private final SessionManager sessionManager;
    private final MessageHistoryService messageHistoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<SessionDto>> create(@Valid @RequestBody CreateSessionRequest request) {
        SessionDto data = SessionDto.from(sessionManager.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionDto>>> getByUser(@RequestParam String userId) {
        List<SessionDto> data = sessionManager.getByUser(userId).stream()
                .map(SessionDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionDto>> get(@PathVariable String id) {
        SessionDto data = sessionManager.get(id)
                .map(SessionDto::from)
                .orElseThrow(this::sessionNotFound);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionDto>> update(@PathVariable String id, @RequestBody UpdateSessionRequest request) {
        SessionDto data = sessionManager.update(id, request)
                .map(SessionDto::from)
                .orElseThrow(this::sessionNotFound);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        sessionManager.delete(id);
        messageHistoryService.clear(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(@PathVariable String id) {
        requireSession(id);

        List<MessageDto> data = messageHistoryService.get(id).stream()
                .map(MessageDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Void>> deleteMessages(@PathVariable String id) {
        requireSession(id);
        messageHistoryService.clear(id);
        return ResponseEntity.noContent().build();
    }

    private void requireSession(String id) {
        sessionManager.get(id).orElseThrow(this::sessionNotFound);
    }

    private ChatbotException sessionNotFound() {
        return new ChatbotException(SESSION_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND.value(), SESSION_NOT_FOUND_CODE);
    }
}
