package com.chatbot.backend.controller;

import com.chatbot.backend.domain.Session;
import com.chatbot.backend.dto.CreateSessionRequest;
import com.chatbot.backend.dto.UpdateSessionRequest;
import com.chatbot.backend.service.MessageHistoryService;
import com.chatbot.backend.service.SessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionManager sessionManager;

    @MockitoBean
    MessageHistoryService messageHistoryService;

    @Test
    @DisplayName("POST /api/sessions: 제목 없이 세션을 생성하면 201과 함께 기본 제목 'New Chat'인 세션을 반환한다")
    void createSession_WhenNoTitle_ThenReturnsCreatedWithDefaultTitle() throws Exception {
        //given
        Session created = session("generated-id", "New Chat");
        when(sessionManager.create(any(CreateSessionRequest.class))).thenReturn(created);

        //when //then
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("generated-id"))
            .andExpect(jsonPath("$.title").value("New Chat"));
    }

    @Test
    @DisplayName("POST /api/sessions: 커스텀 제목으로 세션을 생성하면 201과 함께 그 제목인 세션을 반환한다")
    void createSession_WhenCustomTitle_ThenReturnsCreatedWithTitle() throws Exception {
        //given
        Session created = session("generated-id", "내 챗방");
        when(sessionManager.create(any(CreateSessionRequest.class))).thenReturn(created);

        //when //then
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"내 챗방\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.title").value("내 챗방"));
    }

    @Test
    @DisplayName("GET /api/sessions: 생성된 모든 세션을 배열로 반환한다")
    void getAllSessions_ThenReturnsArrayOfSessions() throws Exception {
        //given
        when(sessionManager.getAll()).thenReturn(List.of(
            session("id-1", "첫번째"),
            session("id-2", "두번째")));

        //when //then
        mockMvc.perform(get("/api/sessions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/sessions/{id}: id로 존재하는 세션을 조회하면 200과 함께 해당 세션을 반환한다")
    void getSession_WhenExists_ThenReturnsSession() throws Exception {
        //given
        Session created = session("session-id", "조회 대상");
        when(sessionManager.get(anyString())).thenReturn(Optional.of(created));

        //when //then
        mockMvc.perform(get("/api/sessions/{id}", "session-id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("session-id"))
            .andExpect(jsonPath("$.title").value("조회 대상"));
    }

    @Test
    @DisplayName("GET /api/sessions/{id}: 존재하지 않는 id로 조회하면 404를 반환한다")
    void getSession_WhenNotExists_ThenReturnsNotFound() throws Exception {
        //given
        when(sessionManager.get(anyString())).thenReturn(Optional.empty());

        //when //then
        mockMvc.perform(get("/api/sessions/{id}", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/sessions/{id}: 세션 제목을 수정하면 200과 함께 변경된 제목인 세션을 반환한다")
    void updateSession_WhenTitleChanged_ThenReturnsUpdatedSession() throws Exception {
        //given
        Session updated = session("session-id", "변경된 제목");
        when(sessionManager.update(anyString(), any(UpdateSessionRequest.class)))
            .thenReturn(Optional.of(updated));

        //when //then
        mockMvc.perform(put("/api/sessions/{id}", "session-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"변경된 제목\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("변경된 제목"));
    }

    @Test
    @DisplayName("DELETE /api/sessions/{id}: 세션을 삭제하면 204를 반환하고 메시지 히스토리도 함께 삭제하며 이후 조회 시 404가 된다")
    void deleteSession_ThenReturnsNoContentAndClearsHistoryAndSubsequentGetReturnsNotFound() throws Exception {
        //given
        when(sessionManager.delete(anyString())).thenReturn(true);
        when(sessionManager.get(anyString())).thenReturn(Optional.empty());

        //when
        mockMvc.perform(delete("/api/sessions/{id}", "session-id"))
            .andExpect(status().isNoContent());

        //then
        verify(messageHistoryService).clear("session-id");
        mockMvc.perform(get("/api/sessions/{id}", "session-id"))
            .andExpect(status().isNotFound());
    }

    private Session session(String id, String title) {
        return new Session(id, title, null);
    }
}
