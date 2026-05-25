package com.chatbot.backend.repository;

import com.chatbot.backend.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m where m.sessionId = :sessionId order by m.id asc")
    List<Message> findAllBySessionId(@Param("sessionId") String sessionId);

    boolean existsBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
