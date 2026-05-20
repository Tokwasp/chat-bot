package com.chatbot.backend.service;

import com.chatbot.backend.domain.Message;
import com.chatbot.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageHistoryService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final MessageRepository repository;
    private final MessageHistoryOptions options;

    @Transactional
    public Message add(String sessionId, String role, String content) {
        Message saved = repository.save(new Message(sessionId, role, content));
        trimPairs(sessionId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Message> get(String sessionId) {
        return repository.findBySessionIdOrderByIdAsc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<Message> getWithLimit(String sessionId, int limit) {
        List<Message> messages = repository.findBySessionIdOrderByIdAsc(sessionId);
        int from = Math.max(0, messages.size() - limit);
        return new ArrayList<>(messages.subList(from, messages.size()));
    }

    @Transactional
    public void clear(String sessionId) {
        repository.deleteBySessionId(sessionId);
    }

    @Transactional
    public boolean delete(String sessionId) {
        if (!repository.existsBySessionId(sessionId)) {
            return false;
        }
        repository.deleteBySessionId(sessionId);
        return true;
    }

    private void trimPairs(String sessionId) {
        Integer max = options.getMaxMessages();
        if (max == null) {
            return;
        }

        List<Message> messages = new ArrayList<>(repository.findBySessionIdOrderByIdAsc(sessionId));
        List<Long> toDelete = new ArrayList<>();

        while (messages.size() > max) {
            Message first = messages.get(0);
            if (messages.size() >= 2
                && ROLE_USER.equals(first.getRole())
                && ROLE_ASSISTANT.equals(messages.get(1).getRole())) {
                toDelete.add(messages.remove(0).getId());
                toDelete.add(messages.remove(0).getId());
                continue;
            }
            toDelete.add(messages.remove(0).getId());
        }

        if (!toDelete.isEmpty()) {
            repository.deleteAllById(toDelete);
        }
    }
}
