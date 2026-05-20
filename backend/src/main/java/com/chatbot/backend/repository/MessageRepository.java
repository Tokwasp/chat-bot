package com.chatbot.backend.repository;

import com.chatbot.backend.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySessionIdOrderByIdAsc(String sessionId);

    boolean existsBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
