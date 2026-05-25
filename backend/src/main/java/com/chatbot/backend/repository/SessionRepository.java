package com.chatbot.backend.repository;

import com.chatbot.backend.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    @Query("select s from Session s where s.userId = :userId order by s.updatedAt desc")
    List<Session> findAllByUserId(@Param("userId") String userId);
}
