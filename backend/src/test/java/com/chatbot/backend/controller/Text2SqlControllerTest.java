package com.chatbot.backend.controller;

import com.chatbot.backend.dto.response.Text2SqlResponse;
import com.chatbot.backend.service.Text2SqlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Text2SqlController.class)
class Text2SqlControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Text2SqlService text2SqlService;

    @Test
    @DisplayName("question이 누락되면 400 에러와 함께 '질문을 입력해주세요.' 메시지를 반환한다")
    void generate_WhenQuestionMissing_ThenReturnsBadRequest() throws Exception {
        //given
        String body = "{}";

        //when //then
        mockMvc.perform(post("/api/text2sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("질문을 입력해주세요."));
    }

    @Test
    @DisplayName("question이 빈 문자열이면 400 에러와 함께 '질문을 입력해주세요.' 메시지를 반환한다")
    void generate_WhenQuestionBlank_ThenReturnsBadRequest() throws Exception {
        //given
        String body = "{\"question\":\"\"}";

        //when //then
        mockMvc.perform(post("/api/text2sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("질문을 입력해주세요."));
    }

    @Test
    @DisplayName("정상 요청이면 200과 함께 intent, entities, sql 3단계 결과를 반환한다")
    void generate_WhenValidRequest_ThenReturnsThreeStepResult() throws Exception {
        //given
        when(text2SqlService.generate(anyString()))
            .thenReturn(new Text2SqlResponse("거래내역 조회", "기간: 지난달", "SELECT * FROM transactions"));
        String body = "{\"question\":\"지난달 거래 내역 보여줘\"}";

        //when //then
        mockMvc.perform(post("/api/text2sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.intent").value("거래내역 조회"))
            .andExpect(jsonPath("$.data.entities").value("기간: 지난달"))
            .andExpect(jsonPath("$.data.sql").value("SELECT * FROM transactions"));
    }
}
