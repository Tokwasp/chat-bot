package com.chatbot.backend.controller;

import com.chatbot.backend.domain.Session;
import com.chatbot.backend.service.MessageHistoryService;
import com.chatbot.backend.service.SessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
class SessionMessagesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionManager sessionManager;

    @MockitoBean
    MessageHistoryService messageHistoryService;

    @Test
    @DisplayName("GET /api/sessions/{id}/messages: 새 세션은 빈 메시지 배열을 반환한다")
    void getMessages_WhenNewSession_ThenReturnsEmptyArray() throws Exception {
        //given
        when(sessionManager.get(anyString())).thenReturn(Optional.of(session("session-id")));
        when(messageHistoryService.get(anyString())).thenReturn(List.of());

        //when //then
        mockMvc.perform(get("/api/sessions/{id}/messages", "session-id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/sessions/{id}/messages: 존재하지 않는 세션은 404를 반환한다")
    void getMessages_WhenSessionNotExists_ThenReturnsNotFound() throws Exception {
        //given
        when(sessionManager.get(anyString())).thenReturn(Optional.empty());

        //when //then
        mockMvc.perform(get("/api/sessions/{id}/messages", "missing-id"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/sessions/{id}/messages: 메시지를 초기화하면 204를 반환한다")
    void deleteMessages_WhenSessionExists_ThenReturnsNoContent() throws Exception {
        //given
        when(sessionManager.get(anyString())).thenReturn(Optional.of(session("session-id")));

        //when //then
        mockMvc.perform(delete("/api/sessions/{id}/messages", "session-id"))
            .andExpect(status().isNoContent());

        verify(messageHistoryService).clear("session-id");
    }

    @Test
    @DisplayName("DELETE /api/sessions/{id}/messages: 존재하지 않는 세션은 404를 반환하고 히스토리를 삭제하지 않는다")
    void deleteMessages_WhenSessionNotExists_ThenReturnsNotFound() throws Exception {
        //given
        when(sessionManager.get(anyString())).thenReturn(Optional.empty());

        //when //then
        mockMvc.perform(delete("/api/sessions/{id}/messages", "missing-id"))
            .andExpect(status().isNotFound());

        verify(messageHistoryService, never()).clear(anyString());
    }

    private Session session(String id) {
        return new Session(id, "user-1", "Messages Test", null);
    }
}
