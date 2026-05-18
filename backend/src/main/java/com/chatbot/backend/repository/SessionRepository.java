package com.chatbot.backend.repository;

import com.chatbot.backend.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    List<Session> findAllByOrderByUpdatedAtDesc();
}
