package com.chatbot.backend.controller;

import com.chatbot.backend.dto.CreateSessionRequest;
import com.chatbot.backend.dto.MessageDto;
import com.chatbot.backend.dto.SessionDto;
import com.chatbot.backend.dto.UpdateSessionRequest;
import com.chatbot.backend.exception.ChatbotException;
import com.chatbot.backend.service.MessageHistoryService;
import com.chatbot.backend.service.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDto create(@RequestBody CreateSessionRequest request) {
        return SessionDto.from(sessionManager.create(request));
    }

    @GetMapping
    public List<SessionDto> getAll() {
        return sessionManager.getAll().stream()
                .map(SessionDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SessionDto get(@PathVariable String id) {
        return sessionManager.get(id)
                .map(SessionDto::from)
                .orElseThrow(this::sessionNotFound);
    }

    @PutMapping("/{id}")
    public SessionDto update(@PathVariable String id, @RequestBody UpdateSessionRequest request) {
        return sessionManager.update(id, request)
                .map(SessionDto::from)
                .orElseThrow(this::sessionNotFound);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        sessionManager.delete(id);
        messageHistoryService.clear(id);
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> getMessages(@PathVariable String id) {
        requireSession(id);

        return messageHistoryService.get(id).stream()
                .map(MessageDto::from)
                .toList();
    }

    @DeleteMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessages(@PathVariable String id) {
        requireSession(id);
        messageHistoryService.clear(id);
    }

    private void requireSession(String id) {
        if (sessionManager.get(id).isEmpty()) {
            throw sessionNotFound();
        }
    }

    private ChatbotException sessionNotFound() {
        return new ChatbotException(SESSION_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND.value(), SESSION_NOT_FOUND_CODE);
    }
}
