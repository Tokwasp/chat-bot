package com.chatbot.backend.service;

import com.chatbot.backend.domain.Session;
import com.chatbot.backend.dto.CreateSessionRequest;
import com.chatbot.backend.dto.UpdateSessionRequest;
import com.chatbot.backend.exception.ChatbotException;
import com.chatbot.backend.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SessionManager {

    private static final String DEFAULT_TITLE = "New Chat";

    private final SessionRepository repository;

    @Transactional
    public Session create(CreateSessionRequest request) {
        if (!StringUtils.hasText(request.getUserId())) {
            throw new ChatbotException("사용자 ID가 필요합니다.", HttpStatus.BAD_REQUEST.value(), "MISSING_USER_ID");
        }
        String id = StringUtils.hasText(request.getId())
                ? request.getId()
                : UUID.randomUUID().toString();
        String title = StringUtils.hasText(request.getTitle())
                ? request.getTitle()
                : DEFAULT_TITLE;

        return repository.save(new Session(id, request.getUserId(), title, request.getMetadata()));
    }

    @Transactional
    public void touch(String id) {
        repository.findById(id).ifPresent(Session::touch);
    }

    @Transactional(readOnly = true)
    public Optional<Session> get(String id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Session> getByUser(String userId) {
        return repository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public Optional<Session> update(String id, UpdateSessionRequest request) {
        Optional<Session> sessionOpt = repository.findById(id);
        sessionOpt.ifPresent(s -> s.applyUpdate(request));
        return sessionOpt;
    }

    @Transactional
    public boolean delete(String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
