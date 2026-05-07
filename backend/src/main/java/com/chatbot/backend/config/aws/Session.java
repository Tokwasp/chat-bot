package com.chatbot.backend.config.aws;

import lombok.Getter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Session {
    private final String sessionId;
    private final List<Message> messages;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Session(String sessionId) {
        this.sessionId = sessionId;
        this.messages = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
    }
}
