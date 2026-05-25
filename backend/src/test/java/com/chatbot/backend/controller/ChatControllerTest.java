package com.chatbot.backend.controller;

import com.chatbot.backend.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    @MockitoBean
    ThreadPoolTaskExecutor streamExecutor;

    @Test
    @DisplayName("sessionId가 누락되면 400 에러와 함께 '세션 ID가 필요합니다.' 메시지를 반환한다")
    void chat_WhenSessionIdMissing_ThenReturnsBadRequest() throws Exception {
        //given
        String body = chatRequestBody(null, "안녕하세요");

        //when //then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("세션 ID가 필요합니다."))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("message가 누락되면 400 에러와 함께 '메시지를 입력해주세요.' 메시지를 반환한다")
    void chat_WhenMessageMissing_ThenReturnsBadRequest() throws Exception {
        //given
        String body = chatRequestBody("test-session", null);

        //when //then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("메시지를 입력해주세요."));
    }

    @Test
    @DisplayName("message가 빈 문자열이면 400 에러와 함께 '메시지를 입력해주세요.' 메시지를 반환한다")
    void chat_WhenMessageBlank_ThenReturnsBadRequest() throws Exception {
        //given
        String body = chatRequestBody("test-session", "");

        //when //then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("메시지를 입력해주세요."));
    }

    @Test
    @DisplayName("정상 요청이면 SSE 형식의 Content-Type(text/event-stream) 헤더를 반환한다")
    void chat_WhenValidRequest_ThenReturnsSseContentType() throws Exception {
        //given
        String body = chatRequestBody("test-session", "안녕하세요");

        //when //then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_EVENT_STREAM_VALUE)));
    }

    private String chatRequestBody(String sessionId, String message) {
        StringBuilder json = new StringBuilder("{");
        if (sessionId != null) {
            json.append("\"sessionId\":\"").append(sessionId).append("\"");
        }
        if (sessionId != null && message != null) {
            json.append(",");
        }
        if (message != null) {
            json.append("\"message\":\"").append(message).append("\"");
        }
        json.append("}");

        return json.toString();
    }
}
